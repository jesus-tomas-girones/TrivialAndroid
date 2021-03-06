/*
 * Copyright 2014 Google Inc.
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
package com.trivial.upv.android.model.quiz;

import android.test.suitebuilder.annotation.SmallTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SmallTest
public class MultiSelectQuizTest extends AbsQuizTestCase<MultiSelectQuiz> {

    @Override
    public void quiz_answer_correctlyStored() {
        assertThat(INT_ARRAY, is(getQuiz().getAnswer()));
    }

    @Override
    public MultiSelectQuiz getQuiz() {
        return new MultiSelectQuiz(QUESTION, INT_ARRAY, STRING_ARRAY, false);
    }

    @Override
    public QuizType getExpectedQuizType() {
        return QuizType.MULTI_SELECT;
    }
}