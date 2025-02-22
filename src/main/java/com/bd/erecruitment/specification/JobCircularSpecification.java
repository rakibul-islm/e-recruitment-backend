package com.bd.erecruitment.specification;

import com.bd.erecruitment.entity.JobCircular;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.Date;

public class JobCircularSpecification {

    public static Specification<JobCircular> findByCriteria(
            String jobTitle,
            String companyName,
            String companyAddress,
            String companyPhone,
            String companyEmail,
            String companyWebsite,
            String companyBusiness,
            Date applicationDeadLine,
            Integer vacancy,
            String experience,
            String salary,
            String jobLocation,
            String jobRequirement,
            String jobResponsibilities,
            String otherBenefits,
            String workPlace,
            String employmentStatus
    ){
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (jobTitle != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("jobTitle")), "%" + jobTitle.toLowerCase() + "%"));
            }
            if (companyName != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("companyName")), "%" + companyName.toLowerCase() + "%"));
            }
            if (companyAddress != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("companyAddress")), "%" + companyAddress.toLowerCase() + "%"));
            }
            if (companyPhone != null) {
                predicate = cb.and(predicate, cb.equal(root.get("companyPhone"), companyPhone));
            }
            if (companyEmail != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("companyEmail")), "%" + companyEmail.toLowerCase() + "%"));
            }
            if (companyWebsite != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("companyWebsite")), "%" + companyWebsite.toLowerCase() + "%"));
            }
            if (companyBusiness != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("companyBusiness")), "%" + companyBusiness.toLowerCase() + "%"));
            }
            if (applicationDeadLine != null) {
                predicate = cb.and(predicate, cb.equal(root.get("applicationDeadLine"), applicationDeadLine));
            }
            if (vacancy != null) {
                predicate = cb.and(predicate, cb.equal(root.get("vacancy"), vacancy));
            }
            if (experience != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("experience")), "%" + experience.toLowerCase() + "%"));
            }
            if (salary != null) {
                predicate = cb.and(predicate, cb.equal(root.get("salary"), salary));
            }
            if (jobLocation != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("jobLocation")), "%" + jobLocation.toLowerCase() + "%"));
            }
            if (jobRequirement != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("jobRequirement")), "%" + jobRequirement.toLowerCase() + "%"));
            }
            if (jobResponsibilities != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("jobResponsibilities")), "%" + jobResponsibilities.toLowerCase() + "%"));
            }
            if (otherBenefits != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("otherBenefits")), "%" + otherBenefits.toLowerCase() + "%"));
            }
            if (workPlace != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("workPlace")), "%" + workPlace.toLowerCase() + "%"));
            }
            if (employmentStatus != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("employmentStatus")), "%" + employmentStatus.toLowerCase() + "%"));
            }
            predicate = cb.and(predicate, cb.equal(root.get("deleted"), false));
            query.orderBy(cb.desc(root.get("id")));

            return predicate;
        };
    }
}
