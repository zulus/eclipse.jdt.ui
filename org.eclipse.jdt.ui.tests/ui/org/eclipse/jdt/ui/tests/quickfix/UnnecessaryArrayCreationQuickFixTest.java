/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.List;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

import junit.framework.Test;
import junit.framework.TestSuite;

public class UnnecessaryArrayCreationQuickFixTest extends QuickFixTest {

	private static final Class<UnnecessaryArrayCreationQuickFixTest> THIS= UnnecessaryArrayCreationQuickFixTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private FixCorrectionProposal fRemoveArrayCreationProposal;

	public UnnecessaryArrayCreationQuickFixTest(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fRemoveArrayCreationProposal= null;
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
		fJProject1= null;
		fSourceFolder= null;
		fRemoveArrayCreationProposal= null;
	}

	public void testMethodCase1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu, 0);

		assertNotNull(fRemoveArrayCreationProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fRemoveArrayCreationProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(\"a\", \"b\", \"c\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMethodCase2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu, 8);

		assertNotNull(fRemoveArrayCreationProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fRemoveArrayCreationProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(\"a\", \"b\", \"c\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMethodCase3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu, 22);

		assertNotNull(fRemoveArrayCreationProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fRemoveArrayCreationProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(\"a\", \"b\", \"c\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testNoMethodProposalCase1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal(buf, cu, -8);

		assertNull(fRemoveArrayCreationProposal);
	}

	public void testNoMethodProposalCase2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo2(String a, String ... b) {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("		foo2(\"a\", \"b\", new String[] {\"c\", \"d\"});\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal3(buf, cu, 0);

		assertNull(fRemoveArrayCreationProposal);
	}

	public void testNoMethodProposalCase3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo2(String a, String ... b) {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("		foo2(\"a\", \"b\", \"c\", \"d\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal3(buf, cu, 0);

		assertNull(fRemoveArrayCreationProposal);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=558614
	public void testNoMethodProposalCase4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> list = Arrays.asList(new String[0]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal(buf, cu, 0);

		assertNull(fRemoveArrayCreationProposal);
	}

	public void testSuperCase1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal2(buf, cu, 0);

		assertNotNull(fRemoveArrayCreationProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fRemoveArrayCreationProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, \"a\", \"b\", \"c\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testSuperCase2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal2(buf, cu, 11);

		assertNotNull(fRemoveArrayCreationProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fRemoveArrayCreationProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, \"a\", \"b\", \"c\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testSuperCase3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal2(buf, cu, 20);

		assertNotNull(fRemoveArrayCreationProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fRemoveArrayCreationProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, \"a\", \"b\", \"c\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testSuperCase4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal2(buf, cu, 29);

		assertNotNull(fRemoveArrayCreationProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fRemoveArrayCreationProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, \"a\", \"b\", \"c\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testNoSuperMethodProposalCase1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, new String[] {\"a\", \"b\", \"c\"});\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal2(buf, cu, -3);

		assertNull(fRemoveArrayCreationProposal);
	}

	public void testNoSuperMethodProposalCase2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, \"a\", new String[] {\"b\", \"c\"});\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal2(buf, cu, 0);

		assertNull(fRemoveArrayCreationProposal);
	}

	public void testNoSuperMethodProposalCase3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, \"a\", \"b\", \"c\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal2(buf, cu, 0);

		assertNull(fRemoveArrayCreationProposal);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=558614
	public void testNoSuperMethodProposalCase4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    private class A1 {\n");
		buf.append("        public String foo(int x, String ... b) {\n");
		buf.append("            return b[0];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class A2 extends A1 {\n");
		buf.append("        public String foo(int x) {\n");
		buf.append("            return super.foo(x, new String[0]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		fetchConvertingProposal2(buf, cu, 0);

		assertNull(fRemoveArrayCreationProposal);
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal(StringBuilder buf, ICompilationUnit cu, int offset) throws Exception {
		int index= buf.toString().indexOf("asList") + offset;
		AssistContext context= getCorrectionContext(cu, index, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		fRemoveArrayCreationProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.REMOVE_UNNECESSARY_ARRAY_CREATION_ID, proposals);
		return proposals;
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal2(StringBuilder buf, ICompilationUnit cu, int offset) throws Exception {
		int index= buf.toString().indexOf("super.foo") + offset;
		AssistContext context= getCorrectionContext(cu, index, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		fRemoveArrayCreationProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.REMOVE_UNNECESSARY_ARRAY_CREATION_ID, proposals);
		return proposals;
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal3(StringBuilder buf, ICompilationUnit cu, int offset) throws Exception {
		int index= buf.toString().indexOf("foo2") + offset;
		AssistContext context= getCorrectionContext(cu, index, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		fRemoveArrayCreationProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.REMOVE_UNNECESSARY_ARRAY_CREATION_ID, proposals);
		return proposals;
	}


}
