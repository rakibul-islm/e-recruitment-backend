package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.StoredFile;

public interface StorageService {

	StoredFile store(String filename, String contentType, byte[] data);

	/** Overwrites the content of an existing file in place, keeping its id. */
	StoredFile replace(Long fileId, String filename, String contentType, byte[] data);

	StoredFile retrieve(Long fileId);

	void delete(Long fileId);
}
