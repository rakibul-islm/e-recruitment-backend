package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.StoredFile;

// Storage abstraction for resumes, generated CVs, offer letters, etc. Backed today by StoredFile
// rows in the app DB (see StorageServiceImpl) - fine at current scale. If file volume ever
// justifies it, swap the implementation for an S3-compatible one without touching callers.
public interface StorageService {

	StoredFile store(String filename, String contentType, byte[] data);

	StoredFile retrieve(Long fileId);

	void delete(Long fileId);
}
