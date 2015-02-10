package com.aol.micro.server.rest.resources;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;

import com.aol.micro.server.events.JobsBeingExecuted;
import com.aol.micro.server.events.RequestsBeingExecuted;
import com.aol.micro.server.events.ScheduledJob;
import com.aol.micro.server.rest.JacksonUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;


public class ActiveResourceTest {

	ActiveResource active;
	RequestsBeingExecuted queries1;
	RequestsBeingExecuted queries2;
	JobsBeingExecuted jobs;
	EventBus bus;
	@Before
	public void setUp() throws Exception {
		bus = new EventBus();
		queries1 = new RequestsBeingExecuted(bus,true);
		queries2 = new RequestsBeingExecuted(bus,"partition");
		jobs = new JobsBeingExecuted(new EventBus(),10);
		active = new ActiveResource(Arrays.asList(queries1, queries2),jobs);
	}

	@Test
	public void testactiveRequests() {
		bus.post(RequestsBeingExecuted.start("query",1l));
		bus.post(RequestsBeingExecuted.start("query",2l,"partition",ImmutableMap.of()));
		assertThat( convert( active.activeRequests(null)).get("events"),is( 1));
		assertThat( convert( active.activeRequests("partition")).get("events"), is( 1 ));
	}

	@Test
	public void whenQueriesWithTheSameIdToDifferentTypesEventsIs1ForBoth(){
		bus.post(RequestsBeingExecuted.start("query",1l));
		bus.post(RequestsBeingExecuted.start("query",1l,"partition",ImmutableMap.of()));
		assertThat( convert( active.activeRequests(null)).get("events"),is( 1));
		assertThat( convert( active.activeRequests("partition")).get("events"), is( 1 ));
	}
	@Test
	public void whenQueriesWithTheSameIdButSameTypesEventsIs2ButSizeIs1(){
		bus.post(RequestsBeingExecuted.start("query",1l));
		bus.post(RequestsBeingExecuted.start("query",1l));
		assertThat( convert( active.activeRequests(null)).get("events"),is( 2));
		Map map = convert( active.activeRequests(null));
		assertThat( ((Map)map.get("active") ).size() , is( 1 ));
	}

	
	@Test
	public void testActiveJobs() throws Throwable {
		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Signature signature = mock(Signature.class);
		when(pjp.getSignature()).thenReturn(signature);
		when(pjp.getTarget()).thenReturn(this);
		when(signature.getDeclaringType()).thenReturn(ScheduledJob.class);
		jobs.aroundScheduledJob(pjp);
		assertThat( convert( active.activeJobs()).get("events"),is( 1));
		
	}
	
	private Map convert(String str){
		return JacksonUtil.convertFromJson( str, Map.class);
	}
}
