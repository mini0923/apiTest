package com.example.demo.openapi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.entity.ApiResult;
import com.example.demo.repository.ApiResultRepository;




@SpringBootTest
public class ApiResultRepositoryTest {

	@Autowired
	ApiResultRepository apiResultRepository;	 
	
	
	@Test
	void 등록()   {

		ApiResult apiResult = ApiResult.builder()
				.apiCallTime(LocalDateTime.now())
				.resultCode("01")
				.resultMsg("OK")
				.totalCount(10)
				.build();
		
		apiResultRepository.save(apiResult);
		
	}
	
	@Test
	void 전체조회 () {
		
		List<ApiResult> list = apiResultRepository.findAll();
		
		System.out.println("게시물조회");
		for(ApiResult api : list) {
			System.out.println(api);
		}
	}
	
	
	@Test
	void 수정() {
		
		Optional<ApiResult> result = apiResultRepository.findById(1);
		
		ApiResult apiResult = result.get();
		
		apiResult.setResultCode("02");
		
		apiResultRepository.save(apiResult);
		
	}
	
	@Test
	void 삭제() {
		apiResultRepository.deleteAll();
	}
	
}
