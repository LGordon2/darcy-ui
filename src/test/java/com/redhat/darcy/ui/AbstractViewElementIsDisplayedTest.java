/*
 Copyright 2014 Red Hat, Inc. and/or its affiliates.

 This file is part of darcy-ui.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redhat.darcy.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.darcy.ui.annotations.Context;
import com.redhat.darcy.ui.annotations.NotRequired;
import com.redhat.darcy.ui.annotations.Require;
import com.redhat.darcy.ui.annotations.RequireAll;
import com.redhat.darcy.ui.api.Locator;
import com.redhat.darcy.ui.api.ViewElement;
import com.redhat.darcy.ui.api.elements.Element;
import com.redhat.darcy.ui.testing.doubles.AlwaysDisplayedLabel;
import com.redhat.darcy.ui.testing.doubles.NeverDisplayedElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AbstractViewElementIsDisplayedTest {
    @Test(expected = NoRequiredElementsException.class)
    public void shouldThrowNoRequiredElementsExceptionIfCalledWithoutAnyAnnotatedElements() {
        ViewElement testView = new AbstractViewElement(mock(Locator.class)) {
            Element element = new AlwaysDisplayedLabel();
        };

        testView.isDisplayed();
    }

    @Test
    public void shouldReturnTrueIfAllRequiredElementsAreDisplayed() {
        ViewElement testView = new AbstractViewElement(mock(Locator.class)) {
            @Require
            private Element test = new AlwaysDisplayedLabel();
        };

        assertTrue("isDisplayed should return true if all required elements are displayed.",
                testView.isDisplayed());
    }

    @Test
    public void shouldReturnFalseIfNotAllRequiredElementsAreDisplayed() {
        ViewElement testView = new AbstractViewElement(mock(Locator.class)) {
            @Require
            private Element displayed = new AlwaysDisplayedLabel();
            @Require
            private Element notDisplayed = new NeverDisplayedElement();
        };

        assertFalse("isDisplayed should return false if not all required elements are displayed.",
                testView.isDisplayed());
    }

    @Test
    public void shouldReturnTrueIfRequireAllIsUsedAndAllElementsAreDisplayed() {
        @RequireAll
        class TestViewElement extends AbstractViewElement {
            private Element displayed = new AlwaysDisplayedLabel();
            private Element displayed2 = new AlwaysDisplayedLabel();

            TestViewElement(Locator parent) {
                super(parent);
            }
        }

        ViewElement testView = new TestViewElement(mock(Locator.class));

        assertTrue("isDisplayed should return true if all required elements are displayed and "
                + "RequireAll annotation is used.", testView.isDisplayed());
    }

    @Test
    public void shouldReturnFalseIfRequireAllIsUsedAndNotAllElementsAreDisplayed() {
        @RequireAll class TestViewElement extends AbstractViewElement {
            private Element displayed = new AlwaysDisplayedLabel();
            private Element notDisplayed = new NeverDisplayedElement();

            TestViewElement(Locator parent) {
                super(parent);
            }
        }

        ViewElement testView = new TestViewElement(mock(Locator.class));

        assertFalse("isDisplayed should return false if not all required elements are displayed " +
                "and RequireAll annotation is used.", testView.isDisplayed());
    }

    @Test
    public void shouldReturnTrueIfOnlyElementNotDisplayedIsNotRequired() {
        @RequireAll class TestViewElement extends AbstractViewElement {
            private Element displayed = new AlwaysDisplayedLabel();
            @NotRequired
            private Element notDisplayed = new NeverDisplayedElement();

            TestViewElement(Locator parent) {
                super(parent);
            }
        }

        ViewElement testView = new TestViewElement(mock(Locator.class));

        assertTrue("isDisplayed should return true if only element not displayed is not required " +
                "when RequireAll annotation is used.", testView.isDisplayed());
    }

    @Test
    public void shouldReturnFalseIfOnlyElementDisplayedIsNotRequired() {
        @RequireAll class TestViewElement extends AbstractViewElement {
            @NotRequired
            private Element displayed = new AlwaysDisplayedLabel();
            private Element notDisplayed = new NeverDisplayedElement();

            TestViewElement(Locator parent) {
                super(parent);
            }
        }

        ViewElement testView = new TestViewElement(mock(Locator.class));

        assertFalse("isDisplayed should return false if only element actually displayed is not " +
                "required when RequireAll annotation is used.", testView.isDisplayed());
    }

    @Test
    public void shouldAllowBeingOverridden() {
        ViewElement testView = new AbstractViewElement(mock(Locator.class)) {
            @Require
            private Element displayed = new AlwaysDisplayedLabel();

            @Override
            public boolean isDisplayed() {
                return super.isDisplayed();
            }
        };

        assertTrue(testView.isDisplayed());
    }

    @Test(expected = TestException.class)
    public void shouldPropagateUncheckedExceptions() {
        Element throwsExceptionOnIsDisplayed = mock(Element.class);
        when(throwsExceptionOnIsDisplayed.isDisplayed()).thenThrow(TestException.class);

        ViewElement testView = new AbstractViewElement(mock(Locator.class)) {
            @Require Element element = throwsExceptionOnIsDisplayed;
        };

        testView.isDisplayed();
    }

    @Test
    public void shouldReturnTrueIfRequiredViewIsDisplayedButNotLoaded() {
        ViewElement mockElement = mock(ViewElement.class);
        when(mockElement.isDisplayed()).thenReturn(true);
        when(mockElement.isLoaded()).thenReturn(false);

        ViewElement testView = new AbstractViewElement(mock(Locator.class)) {
            @Require
            ViewElement element = mockElement;
        };

        assertTrue("Expected ViewElement to be displayed due to single required field that is a " +
                "displayed view.", testView.isDisplayed());
    }

    // This scenario doesn't make any sense, but serves to prove we're not looking at isLoaded for
    // isDisplayed.
    @Test
    public void shouldReturnFalseIfRequiredViewIsNotDisplayedButIsLoadedAndPresent() {
        ViewElement mockElement = mock(ViewElement.class);
        when(mockElement.isDisplayed()).thenReturn(false);
        when(mockElement.isLoaded()).thenReturn(true);
        when(mockElement.isPresent()).thenReturn(true);

        ViewElement testView = new AbstractViewElement(mock(Locator.class)) {
            @Require
            ViewElement element = mockElement;
        };

        assertFalse("Expected ViewElement to not be displayed due to single required field that " +
                "is a not displayed view.", testView.isDisplayed());
    }

    @Test(expected = NoRequiredElementsException.class)
    public void shouldIgnoreFieldsAnnotatedWithContext() {
        @RequireAll class TestViewElement extends AbstractViewElement {
            @Context
            Element element = mock(Element.class);

            public TestViewElement() {
                super(mock(Locator.class));
            }
        };

        TestViewElement testView = new TestViewElement();
        testView.isDisplayed();
    }

    class TestException extends RuntimeException {}
}
