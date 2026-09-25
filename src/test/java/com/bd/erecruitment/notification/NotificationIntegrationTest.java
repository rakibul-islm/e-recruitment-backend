package com.bd.erecruitment.notification;

import com.bd.erecruitment.dto.res.NotificationPollResDTO;
import com.bd.erecruitment.dto.res.NotificationResDTO;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.impl.NotificationServiceImpl;
import com.bd.erecruitment.util.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NotificationIntegrationTest {

	private static final String USER_A_EMAIL = "test@e-recruitment.com";
	private static final String USER_B_EMAIL = "admin@e-recruitment.com";

	@Autowired
	private NotificationPublisher publisher;

	@Autowired
	private NotificationServiceImpl notificationService;

	@Autowired
	private NotificationRepo notificationRepo;

	@Autowired
	private UserRepo userRepo;

	private Long userAId;
	private Long userBId;

	@BeforeEach
	void setUp() {
		notificationRepo.deleteAll();
		userAId = userRepo.findByEmail(USER_A_EMAIL).getId();
		userBId = userRepo.findByEmail(USER_B_EMAIL).getId();
		authenticateAs(userAId, USER_A_EMAIL);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		notificationRepo.deleteAll();
	}

	@Test
	@Transactional
	void publishedInsideTransaction_createsNotificationOnlyAfterCommit() throws InterruptedException {
		publisher.notifyUser(userAId, NotificationType.OFFER_RECEIVED, "/my/applications/7", "jobTitle", "Java Developer");

		Thread.sleep(300);
		assertThat(rowsFor(userAId)).isEmpty();

		TestTransaction.flagForCommit();
		TestTransaction.end();

		Notification row = awaitRows(userAId, 1).get(0);
		assertThat(row.getType()).isEqualTo(NotificationType.OFFER_RECEIVED);
		assertThat(row.getActionRoute()).isEqualTo("/my/applications/7");
		assertThat(row.getParamsJson()).contains("Java Developer");
		assertThat(row.getReadOn()).isNull();
	}

	@Test
	@Transactional
	void rolledBackTransaction_neverCreatesNotification() throws InterruptedException {
		publisher.notifyUser(userAId, NotificationType.OFFER_RECEIVED, "/my/applications/7");

		TestTransaction.flagForRollback();
		TestTransaction.end();

		Thread.sleep(300);
		assertThat(rowsFor(userAId)).isEmpty();
	}

	@Test
	void notifyEmail_resolvesRecipientByEmail() throws InterruptedException {
		publisher.notifyEmail(USER_B_EMAIL, NotificationType.NEW_APPLICATION, "/application-management/3", "candidateName", "Rahim");

		assertThat(awaitRows(userBId, 1)).hasSize(1);
		assertThat(rowsFor(userAId)).isEmpty();
	}

	@Test
	void notifyEmail_unknownOrBlankEmail_createsNothing() throws InterruptedException {
		publisher.notifyEmail("nobody@example.com", NotificationType.NEW_APPLICATION, "/x");
		publisher.notifyEmail(" ", NotificationType.NEW_APPLICATION, "/x");
		publisher.notifyEmail(null, NotificationType.NEW_APPLICATION, "/x");

		Thread.sleep(500);
		assertThat(notificationRepo.count()).isZero();
	}

	@Test
	void notifyAuthority_reachesEveryHolderIncludingSuperAdmin_andNobodyElse() throws InterruptedException {
		publisher.notifyAuthority("recruiter-application:write", NotificationType.RECRUITER_APPLICATION_SUBMITTED,
				"/recruiter-applications/5", "organizationName", "Acme");

		assertThat(awaitRows(userBId, 1)).hasSize(1);
		assertThat(rowsFor(userAId)).isEmpty();
	}

	@Test
	void sameDedupeKey_createsOnlyOneNotificationPerRecipient() {
		NotificationEvent reminder = event(userAId, "/a1").withDedupeKey("REMINDER:1:100");

		notificationService.create(reminder);
		notificationService.create(reminder);
		notificationService.create(event(userBId, "/b1").withDedupeKey("REMINDER:1:100"));

		assertThat(rowsFor(userAId)).hasSize(1);
		assertThat(rowsFor(userBId)).hasSize(1);
	}

	@Test
	void differentOrMissingDedupeKey_isNeverSuppressed() {
		notificationService.create(event(userAId, "/a1").withDedupeKey("REMINDER:1:100"));
		notificationService.create(event(userAId, "/a2").withDedupeKey("REMINDER:1:200"));
		notificationService.create(event(userAId, "/a3"));
		notificationService.create(event(userAId, "/a4"));

		assertThat(rowsFor(userAId)).hasSize(4);
	}

	@Test
	void absoluteOrProtocolRelativeRoute_isNotStored() {
		notificationService.create(event(userAId, "https://evil.example.com/phish"));
		notificationService.create(event(userAId, "//evil.example.com"));
		notificationService.create(event(userAId, "/my/applications/1"));

		List<String> routes = rowsFor(userAId).stream().map(Notification::getActionRoute).toList();
		assertThat(routes).containsExactlyInAnyOrder(null, null, "/my/applications/1");
	}

	@Test
	void veryLongParam_isTruncatedAndStillFits() {
		notificationService.create(NotificationEvent.toUser(userAId, NotificationType.APPLICATION_RECEIVED, "/x", "jobTitle", "T".repeat(5000)));

		Notification row = rowsFor(userAId).get(0);
		assertThat(row.getParamsJson().length()).isLessThan(2000);
	}

	@Test
	void pollReportsUnreadCountAndLatestId_andMarkReadIsScopedToCurrentUser() {
		notificationService.create(event(userAId, "/a1"));
		notificationService.create(event(userAId, "/a2"));
		notificationService.create(event(userBId, "/b1"));
		Long firstA = rowsFor(userAId).get(1).getId();
		Long secondA = rowsFor(userAId).get(0).getId();
		Long onlyB = rowsFor(userBId).get(0).getId();

		NotificationPollResDTO poll = notificationService.poll().getObj();
		assertThat(poll.getUnreadCount()).isEqualTo(2);
		assertThat(poll.getLatestId()).isEqualTo(secondA);

		notificationService.markRead(firstA);
		assertThat(notificationService.poll().getObj().getUnreadCount()).isEqualTo(1);

		assertThatThrownBy(() -> notificationService.markRead(onlyB)).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> notificationService.remove(onlyB)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void markAllRead_onlyAffectsCurrentUser() {
		notificationService.create(event(userAId, "/a1"));
		notificationService.create(event(userAId, "/a2"));
		notificationService.create(event(userBId, "/b1"));

		notificationService.markAllRead();

		assertThat(notificationService.poll().getObj().getUnreadCount()).isZero();
		authenticateAs(userBId, USER_B_EMAIL);
		assertThat(notificationService.poll().getObj().getUnreadCount()).isEqualTo(1);
	}

	@Test
	void myList_isNewestFirst_pagesByBeforeId_andHidesRemoved() {
		for (int i = 1; i <= 5; i++) notificationService.create(event(userAId, "/a" + i));

		Response<NotificationResDTO> firstPage = notificationService.myList(Long.MAX_VALUE, 2, false);
		assertThat(firstPage.getList()).extracting(NotificationResDTO::getActionRoute).containsExactly("/a5", "/a4");

		Long cursor = firstPage.getList().get(1).getId();
		Response<NotificationResDTO> secondPage = notificationService.myList(cursor, 2, false);
		assertThat(secondPage.getList()).extracting(NotificationResDTO::getActionRoute).containsExactly("/a3", "/a2");

		notificationService.remove(secondPage.getList().get(0).getId());
		Response<NotificationResDTO> afterRemove = notificationService.myList(cursor, 2, false);
		assertThat(afterRemove.getList()).extracting(NotificationResDTO::getActionRoute).containsExactly("/a2", "/a1");
	}

	@Test
	void myList_unreadOnly_skipsReadItems() {
		notificationService.create(event(userAId, "/a1"));
		notificationService.create(event(userAId, "/a2"));
		Long newest = rowsFor(userAId).get(0).getId();
		notificationService.markRead(newest);

		Response<NotificationResDTO> unread = notificationService.myList(Long.MAX_VALUE, 20, true);
		assertThat(unread.getList()).extracting(NotificationResDTO::getActionRoute).containsExactly("/a1");
	}

	private NotificationEvent event(Long userId, String route) {
		return NotificationEvent.toUser(userId, NotificationType.APPLICATION_RECEIVED, route, "jobTitle", "Engineer");
	}

	private List<Notification> rowsFor(Long userId) {
		return notificationRepo.findByRecipientUserIdAndDeletedAndIdLessThanOrderByIdDesc(userId, false, Long.MAX_VALUE, PageRequest.of(0, 50));
	}

	private List<Notification> awaitRows(Long userId, int expected) throws InterruptedException {
		for (int i = 0; i < 100 && rowsFor(userId).size() < expected; i++) {
			Thread.sleep(100);
		}
		return rowsFor(userId);
	}

	private void authenticateAs(Long id, String email) {
		User user = User.builder().id(id).email(email).active(true).expiryDate(farFutureDate()).build();
		MyUserDetail principal = new MyUserDetail(user);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	private Date farFutureDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 10);
		return cal.getTime();
	}
}
