package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.springframework.data.domain.Pageable;

import java.util.Date;

public interface JobCircularService<R, E> extends BaseService<R, E>{

    public Response<R> filter(
            Pageable pageable,
            Boolean isPageable,
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
    ) throws ServiceException;
}
