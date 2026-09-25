package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.StoredFile;

public interface StorageService {

	StoredFile store(String filename, String contentType, byte[] data);

	StoredFile retrieve(Long fileId);

	void delete(Long fileId);
}
