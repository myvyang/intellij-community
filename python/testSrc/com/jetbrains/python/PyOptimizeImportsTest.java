/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python;

import com.intellij.codeInsight.actions.OptimizeImportsAction;
import com.intellij.ide.DataManager;
import com.jetbrains.python.fixtures.PyTestCase;

/**
 * @author yole
 */
public class PyOptimizeImportsTest extends PyTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // importsFromTypingUnusedInTypeComments depends on registered TokenSetContributors
    PythonDialectsTokenSetProvider.reset();
  }

  public void testSimple() {
    doTest();
  }

  public void testOneOfMultiple() {
    doTest();
  }

  public void testImportStar() {
    doTest();
  }

  public void testImportStarOneOfMultiple() {
    doTest();
  }

  public void testTryExcept() {
    doTest();
  }

  public void testFromFuture() {
    doTest();
  }

  public void testUnresolved() {  // PY-2201
    doTest();
  }
  
  public void testSuppressed() {  // PY-5228
    doTest();
  }

  public void testSplit() {
    doTest();
  }

  public void testOrderByType() {
    doTest();
  }

  // PY-12018
  public void testAlphabeticalOrder() {
    doTest();
  }

  public void testInsertBlankLines() {  // PY-8355
    doTest();
  }

  // PY-16351
  public void testNoExtraBlankLineAfterImportBlock() {
    final String testName = getTestName(true);
    myFixture.copyDirectoryToProject(testName, "");
    myFixture.configureByFile("main.py");
    OptimizeImportsAction.actionPerformedImpl(DataManager.getInstance().getDataContext(myFixture.getEditor().getContentComponent()));
    myFixture.checkResultByFile(testName + "/main.after.py");
  }

  // PY-18521
  public void testImportsFromTypingUnusedInTypeComments() {
    myFixture.copyDirectoryToProject("../typing", "");
    doTest();
  }

  private void doTest() {
    myFixture.configureByFile(getTestName(true) + ".py");
    OptimizeImportsAction.actionPerformedImpl(DataManager.getInstance().getDataContext(myFixture.getEditor().getContentComponent()));
    myFixture.checkResultByFile(getTestName(true) + ".after.py");
  }

  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/optimizeImports";
  }
}
