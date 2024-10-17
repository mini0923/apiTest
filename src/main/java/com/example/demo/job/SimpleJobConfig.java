package com.example.demo.job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.entity.ApiResult;
import com.example.demo.repository.ApiResultRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


@Configuration
public class SimpleJobConfig {
	
	@Autowired
	JobRepository jobRepository;
	
	@Autowired
	PlatformTransactionManager manager;
	
	@Autowired
	ApiResultRepository apiResultRepository;
	
	
	String serviceKey = "5yzy5aVUxIgi%2FhIpQJEIrTQWT7Gy6xT4DdLCZ51xkymH4EoW3hV%2BcGxdcj77ZQc7LzMNwWINhhL869rGJ1ebkg%3D%3D";	
	String dataType = "JSON";
	String code = "11B20201";
	
	public String getWeather() throws IOException {
	       StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1360000/VilageFcstMsgService/getLandFcst"); /*URL*/
	        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" +serviceKey); /*Service Key*/
	        urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
	        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("10", "UTF-8")); /*한 페이지 결과 수*/
	        urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=" + URLEncoder.encode(dataType, "UTF-8")); /*요청자료형식(XML/JSON) Default: XML*/
	        urlBuilder.append("&" + URLEncoder.encode("regId","UTF-8") + "=" + URLEncoder.encode(code, "UTF-8")); /*11A00101(백령도), 11B10101 (서울), 11B20201(인천) 등... 별첨 엑셀자료 참조(‘육상’ 구분 값 참고)*/
	        URL url = new URL(urlBuilder.toString());
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("Content-type", "application/json");
	        System.out.println("Response code: " + conn.getResponseCode());
	        BufferedReader rd;
	        if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
	            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        } else {
	            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
	        }
	        StringBuilder sb = new StringBuilder();
	        String line;
	        while ((line = rd.readLine()) != null) {
	            sb.append(line);
	        }
	        rd.close();
	        conn.disconnect();
	        System.out.println(sb.toString());
	        
	        return sb.toString();
	}
	
	
		@Bean 
		public Job simpleJob1() {
			Job job = new JobBuilder("simpleJob" , jobRepository)
																												.start(step1())
																												.next(step2())
																												.next(step3())
																												.build();
			
			return job;
		}
		
		@Bean
		public Step step1() {
			
			Step step = new StepBuilder("step1..", jobRepository)
																.tasklet(testTasklet(), manager)
																.build();
			
			return step;
		}
	
		@Bean
		public Step step2() {
			
				Step step = new StepBuilder("step2..", jobRepository)
																		.tasklet(testTasklet2(), manager)
																		.build();
				
				return step;
		}
	
		@Bean
		public Step step3() {
			
				Step step = new StepBuilder("step3..", jobRepository)
																		.tasklet(testTasklet3(), manager)
																		.build();
				
				return step;
		}
		
		@Bean
		public Tasklet testTasklet() {
			
					Tasklet tasklet = (StepContribution contribution, ChunkContext chunkContext) -> {
					
						System.out.println("step1.. API 호출하기" );

						
						// 매퍼 객체 생성
						ObjectMapper mapper = new ObjectMapper();
						
						// 분석할 수 없는 구문을 무시하는 옵션 설정
						mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
						
						// 날씨 데이터 가져오기
						String weather = getWeather();
						
						Root root = null;
						
						// JSON 문자열을 클래스로 변환
						// 원본데이터, 변환할 클래스
						root = mapper.readValue(weather, Root.class);
						
						System.out.println("Response code  : "+ root.response);
						
						// 다음 스텝에 값을 전달하기 위해
						// 컨텍스트 안에 값을 저장
						StepContext context = chunkContext.getStepContext();
						
						ExecutionContext executionContext = context
																													.getStepExecution()
																													.getJobExecution()
																													.getExecutionContext();
						
						executionContext.put("resultCode", root.response.header.resultCode);
						executionContext.put("resultMsg", root.response.header.resultMsg);
						executionContext.put("totalCount", root.response.body.totalCount);
						
						
			            return RepeatStatus.FINISHED;
						
					};
				
					return tasklet;
		}
		
		
		@Bean
		public Tasklet testTasklet2() {
			
					Tasklet tasklet = (StepContribution contribution, ChunkContext chunkContext) -> {
					
						System.out.println("step2. 응답 데이터 파싱하기");
						
						StepContext context = chunkContext.getStepContext();
						
						ExecutionContext executionContext = context
																													.getStepExecution()
																													.getJobExecution()
																													.getExecutionContext();
						
						String resultCode = executionContext.get("resultCode").toString();
						String resultMsg = executionContext.get("resultMsg").toString();
						String totalCount = executionContext.get("totalCount").toString();
						
						int totalCountNum = Integer.parseInt(totalCount);
						
						executionContext.put("resultCode", resultCode);
						executionContext.put("resultMsg", resultMsg);
						executionContext.put("totalCount", totalCountNum);
						
						System.out.println("resultCode : " + resultCode);
						System.out.println("resultMsg : " + resultMsg);
						System.out.println("totalCount : " + totalCountNum);
						
						
						return RepeatStatus.FINISHED;
						
					};
				
					return tasklet;
		}
		
		@Bean
		public Tasklet testTasklet3() {
			
					Tasklet tasklet = (StepContribution contribution, ChunkContext chunkContext) -> {
					
						System.out.println("step3.  API 호출 결과를 테이블에 저장하기");
						
						StepContext context = chunkContext.getStepContext();
						
						ExecutionContext executionContext = context
																													.getStepExecution()
																													.getJobExecution()
																													.getExecutionContext();
						
						String resultCode = executionContext.get("resultCode").toString();
						String resultMsg = executionContext.get("resultMsg").toString();
						String totalCount = executionContext.get("totalCount").toString();
						
						int totalCountNum = Integer.parseInt(totalCount);

												
						ApiResult apiResult = ApiResult.builder()
								.apiCallTime(LocalDateTime.now())
								.resultCode(resultCode)
								.resultMsg(resultMsg)
								.totalCount(totalCountNum)
								.build();
						
						apiResultRepository.save(apiResult);
						
						return RepeatStatus.FINISHED;
						
					};
				
					return tasklet;
		}
		
	
}
