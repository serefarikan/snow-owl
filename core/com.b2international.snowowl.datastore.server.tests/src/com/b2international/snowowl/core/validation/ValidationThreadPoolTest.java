/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.core.validation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Test;

import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.core.internal.validation.ValidationRuleSchedulingRule;
import com.b2international.snowowl.core.internal.validation.ValidationThreadPool;
import com.b2international.snowowl.core.validation.rule.ValidationRule.CheckType;
import com.google.common.collect.Lists;

/**
 * @since 6.6
 */
public class ValidationThreadPoolTest {

	private static final int MAXIMUM_AMOUNT_OF_RUNNING_EXPENSIVE_JOBS = 1;

	private static final int MAXIMUM_AMOUNT_OF_RUNNING_NORMAL_JOBS = ValidationRuleSchedulingRule.MAXIMUM_AMOUNT_OF_CONCURRENT_NORMAL_JOBS;

	private static final int VALIDATION_THREAD_COUNT = 6;

	@Test
	public void testConcurrentExpensiveJobs() {
		final ValidationThreadPool pool = new ValidationThreadPool(VALIDATION_THREAD_COUNT);

		final IJobManager manager = Job.getJobManager();
		final Runnable expensiveRunnable = createValidatableRunnable(CheckType.EXPENSIVE, manager);

		final List<Promise<Object>> validationPromises = Lists.newArrayList();
		for (int i = 0; i < 10; i++) {
			validationPromises.add(pool.submit(CheckType.EXPENSIVE, expensiveRunnable));
		}

		Promise.all(validationPromises).getSync();
	}

	@Test
	public void testConcurrentFastJobs() {

		final ValidationThreadPool pool = new ValidationThreadPool(VALIDATION_THREAD_COUNT);

		final IJobManager manager = Job.getJobManager();

		final Runnable fastRunnable = createValidatableRunnable(CheckType.FAST, manager);

		final List<Promise<Object>> validationPromises = Lists.newArrayList();
		for (int i = 0; i < 10; i++) {
			validationPromises.add(pool.submit(CheckType.FAST, fastRunnable));
		}

		Promise.all(validationPromises).getSync();
	}

	@Test
	public void TestConcurrentNormalJobs() {
		final ValidationThreadPool pool = new ValidationThreadPool(VALIDATION_THREAD_COUNT);

		final IJobManager manager = Job.getJobManager();

		final Runnable fastRunnable = createValidatableRunnable(CheckType.NORMAL, manager);

		final List<Promise<Object>> validationPromises = Lists.newArrayList();
		for (int i = 0; i < 10; i++) {
			validationPromises.add(pool.submit(CheckType.NORMAL, fastRunnable));
		}

		Promise.all(validationPromises).getSync();
	}

	@Test
	public void testConcurrentFastJobsWithExpensiveOnes() {
		final ValidationThreadPool pool = new ValidationThreadPool(VALIDATION_THREAD_COUNT);

		final IJobManager manager = Job.getJobManager();

		final List<Promise<Object>> validationPromises = Lists.newArrayList();
		for (int i = 0; i < 10; i++) {
			validationPromises.add(pool.submit(CheckType.FAST, createValidatableRunnable(CheckType.FAST, manager)));
			if (i % 3 == 0) {
				validationPromises.add(pool.submit(CheckType.EXPENSIVE, createValidatableRunnable(CheckType.EXPENSIVE, manager)));
			}
		}

		Promise.all(validationPromises).getSync();
	}
	
	@Test
	public void testConcurrentNormalJobsWithFastOnes() {
		final ValidationThreadPool pool = new ValidationThreadPool(VALIDATION_THREAD_COUNT);

		final IJobManager manager = Job.getJobManager();

		final List<Promise<Object>> validationPromises = Lists.newArrayList();
		for (int i = 0; i < 10; i++) {
			validationPromises.add(pool.submit(CheckType.NORMAL, createValidatableRunnable(CheckType.FAST, manager)));
			if (i % 2 == 0) {
				validationPromises.add(pool.submit(CheckType.FAST, createValidatableRunnable(CheckType.FAST, manager)));
			}
		}

		Promise.all(validationPromises).getSync();
	}

	@Test
	public void test50JobsOfAllTypes() {
		final ValidationThreadPool pool = new ValidationThreadPool(VALIDATION_THREAD_COUNT);

		final IJobManager manager = Job.getJobManager();

		final List<Promise<Object>> validationPromises = Lists.newArrayList();
		validationPromises.add(pool.submit(CheckType.EXPENSIVE, createValidatableRunnable(CheckType.EXPENSIVE, manager)));
		for (int i = 0; i < 10; i++) {
			validationPromises.add(pool.submit(CheckType.FAST, createValidatableRunnable(CheckType.FAST, manager)));
			validationPromises.add(pool.submit(CheckType.NORMAL, createValidatableRunnable(CheckType.NORMAL, manager)));
			if (i % 3 == 0) {
				validationPromises.add(pool.submit(CheckType.EXPENSIVE, createValidatableRunnable(CheckType.EXPENSIVE, manager)));
			}
		}

		Promise.all(validationPromises).getSync();
	}

	private Runnable createValidatableRunnable(CheckType checkType, IJobManager manager) {
		return () -> {
			try {
				long runningExpensiveJobs = Arrays.asList(manager.find(CheckType.EXPENSIVE.getName())).stream().filter(job -> job.getState() == Job.RUNNING).count();
				long runningNormalJobs = Arrays.asList(manager.find(CheckType.NORMAL.getName())).stream().filter(job -> job.getState() == Job.RUNNING).count();
				long runningFastJobs = Arrays.asList(manager.find(CheckType.FAST.getName())).stream().filter(job -> job.getState() == Job.RUNNING).count();

				long allRunningJobs = runningExpensiveJobs + runningFastJobs + runningNormalJobs;

				assertTrue(allRunningJobs <= VALIDATION_THREAD_COUNT);

				if (CheckType.EXPENSIVE == checkType) {
					assertTrue(runningExpensiveJobs == MAXIMUM_AMOUNT_OF_RUNNING_EXPENSIVE_JOBS);
				}

				if (CheckType.NORMAL == checkType) {
					assertTrue(runningNormalJobs <= MAXIMUM_AMOUNT_OF_RUNNING_NORMAL_JOBS);
				}
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		};
	}

}
