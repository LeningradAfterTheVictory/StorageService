package org.example.storageservice;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.example.storageservice.application.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StorageServiceTest {

	@Mock
	private AmazonS3 s3Client;

	@InjectMocks
	private StorageService storageService;

	private final String bucketName = "test-bucket";

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(storageService, "bucketName", bucketName);

		// Настраиваем stub для getUrl. Если ключ пустой, возвращаем базовый URL бакета, иначе формируем фиктивный URL с ключом.
		when(s3Client.getUrl(anyString(), anyString())).thenAnswer(invocation -> {
			String bucket = invocation.getArgument(0);
			String key = invocation.getArgument(1);
			if (key == null || key.isEmpty()) {
				return new URL("http://dummy.com/" + bucket + "/");
			}
			return new URL("http://dummy.com/" + key);
		});
	}

	@Test
	void saveFile_shouldUploadFile() {
		// Arrange
		MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
		when(s3Client.putObject(any(PutObjectRequest.class)))
				.thenReturn(new PutObjectResult());

		// Act
		String result = storageService.saveFile(mockFile);

		// Assert
		assertNotNull(result);
		// Проверяем, что URL содержит имя файла (так как уникальное имя генерируется, проверяем включение части оригинального имени)
		assertTrue(result.contains("test.txt"));
		verify(s3Client, times(1)).putObject(any(PutObjectRequest.class));
	}

	@Test
	void saveFilesShouldUploadMultipleFiles() {
		// Arrange
		List<MultipartFile> files = List.of(
				new MockMultipartFile("file1", "test1.txt", "text/plain", "Content 1".getBytes()),
				new MockMultipartFile("file2", "test2.txt", "text/plain", "Content 2".getBytes())
		);
		when(s3Client.putObject(any(PutObjectRequest.class)))
				.thenReturn(new PutObjectResult());

		// Act
		List<String> result = storageService.saveFiles(files);

		// Assert
		assertEquals(2, result.size());
		verify(s3Client, times(2)).putObject(any(PutObjectRequest.class));
	}

	@Test
	void deleteFileShouldRemoveFile() {
		// Arrange
		String fileUrl = "http://dummy.com/test-bucket/test.txt";

		// Act
		storageService.deleteFile(fileUrl);

		// Assert
		verify(s3Client, times(1)).deleteObject(bucketName, "test.txt");
	}

	@Test
	void listFilesShouldReturnFileList() {
		// Arrange
		String folderPath = "test-folder/";
		List<S3ObjectSummary> objectSummaries = new ArrayList<>();
		S3ObjectSummary summary1 = new S3ObjectSummary();
		summary1.setKey(folderPath + "file1.txt");
		S3ObjectSummary summary2 = new S3ObjectSummary();
		summary2.setKey(folderPath + "file2.txt");
		objectSummaries.add(summary1);
		objectSummaries.add(summary2);

		// Создаём фиктивный результат listObjectsV2
		ListObjectsV2Result listResult = new ListObjectsV2Result();
		listResult.getObjectSummaries().addAll(objectSummaries);
		// Stub для вызова, принимающего ListObjectsV2Request
		when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResult);

		// Act
		List<String> result = storageService.listFiles(folderPath);

		// Assert
		assertEquals(2, result.size());
		assertTrue(result.get(0).contains("file1.txt"));
		assertTrue(result.get(1).contains("file2.txt"));
	}
}