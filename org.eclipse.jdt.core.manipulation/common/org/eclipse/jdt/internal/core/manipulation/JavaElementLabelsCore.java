/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - create JavaElementLabelsCore
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import org.eclipse.osgi.util.TextProcessor;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;


/**
 * <code>JavaElementLabels</code> provides helper methods to render names of Java elements.
 *
 * @since 3.1
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class JavaElementLabelsCore {


	/**
	 * Method names contain parameter types.
	 * e.g. <code>foo(int)</code>
	 */
	public final static long M_PARAMETER_TYPES= 1L << 0;

	/**
	 * Method names contain parameter names.
	 * e.g. <code>foo(index)</code>
	 */
	public final static long M_PARAMETER_NAMES= 1L << 1;

	/**
	 * Method names contain type parameters prepended.
	 * e.g. <code>&lt;A&gt; foo(A index)</code>
	 */
	public final static long M_PRE_TYPE_PARAMETERS= 1L << 2;

	/**
	 * Method names contain type parameters appended.
	 * e.g. <code>foo(A index) &lt;A&gt;</code>
	 */
	public final static long M_APP_TYPE_PARAMETERS= 1L << 3;

	/**
	 * Method names contain thrown exceptions.
	 * e.g. <code>foo throws IOException</code>
	 */
	public final static long M_EXCEPTIONS= 1L << 4;

	/**
	 * Method names contain return type (appended)
	 * e.g. <code>foo : int</code>
	 */
	public final static long M_APP_RETURNTYPE= 1L << 5;

	/**
	 * Method names contain return type (appended)
	 * e.g. <code>int foo</code>
	 */
	public final static long M_PRE_RETURNTYPE= 1L << 6;

	/**
	 * Method names are fully qualified.
	 * e.g. <code>java.util.Vector.size</code>
	 */
	public final static long M_FULLY_QUALIFIED= 1L << 7;

	/**
	 * Method names are post qualified.
	 * e.g. <code>size - java.util.Vector</code>
	 */
	public final static long M_POST_QUALIFIED= 1L << 8;

	/**
	 * Initializer names are fully qualified.
	 * e.g. <code>java.util.Vector.{ ... }</code>
	 */
	public final static long I_FULLY_QUALIFIED= 1L << 10;

	/**
	 * Type names are post qualified.
	 * e.g. <code>{ ... } - java.util.Map</code>
	 */
	public final static long I_POST_QUALIFIED= 1L << 11;

	/**
	 * Field names contain the declared type (appended)
	 * e.g. <code>fHello : int</code>
	 */
	public final static long F_APP_TYPE_SIGNATURE= 1L << 14;

	/**
	 * Field names contain the declared type (prepended)
	 * e.g. <code>int fHello</code>
	 */
	public final static long F_PRE_TYPE_SIGNATURE= 1L << 15;

	/**
	 * Fields names are fully qualified.
	 * e.g. <code>java.lang.System.out</code>
	 */
	public final static long F_FULLY_QUALIFIED= 1L << 16;

	/**
	 * Fields names are post qualified.
	 * e.g. <code>out - java.lang.System</code>
	 */
	public final static long F_POST_QUALIFIED= 1L << 17;

	/**
	 * Type names are fully qualified.
	 * e.g. <code>java.util.Map.Entry</code>
	 */
	public final static long T_FULLY_QUALIFIED= 1L << 18;

	/**
	 * Type names are type container qualified.
	 * e.g. <code>Map.Entry</code>
	 */
	public final static long T_CONTAINER_QUALIFIED= 1L << 19;

	/**
	 * Type names are post qualified.
	 * e.g. <code>Entry - java.util.Map</code>
	 */
	public final static long T_POST_QUALIFIED= 1L << 20;

	/**
	 * Type names contain type parameters.
	 * e.g. <code>Map&lt;S, T&gt;</code>
	 */
	public final static long T_TYPE_PARAMETERS= 1L << 21;

	/**
	 * Type parameters are post qualified.
	 * e.g. <code>K - java.util.Map.Entry</code>
	 *
	 * @since 3.5
	 */
	public final static long TP_POST_QUALIFIED= 1L << 22;

	/**
	 * Declarations (import container / declaration, package declaration) are qualified.
	 * e.g. <code>java.util.Vector.class/import container</code>
	 */
	public final static long D_QUALIFIED= 1L << 24;

	/**
	 * Declarations (import container / declaration, package declaration) are post qualified.
	 * e.g. <code>import container - java.util.Vector.class</code>
	 */
	public final static long D_POST_QUALIFIED= 1L << 25;

	/**
	 * Class file names are fully qualified.
	 * e.g. <code>java.util.Vector.class</code>
	 */
	public final static long CF_QUALIFIED= 1L << 27;

	/**
	 * Class file names are post qualified.
	 * e.g. <code>Vector.class - java.util</code>
	 */
	public final static long CF_POST_QUALIFIED= 1L << 28;

	/**
	 * Compilation unit names are fully qualified.
	 * e.g. <code>java.util.Vector.java</code>
	 */
	public final static long CU_QUALIFIED= 1L << 31;

	/**
	 * Compilation unit names are post  qualified.
	 * e.g. <code>Vector.java - java.util</code>
	 */
	public final static long CU_POST_QUALIFIED= 1L << 32;

	/**
	 * Package names are qualified.
	 * e.g. <code>MyProject/src/java.util</code>
	 */
	public final static long P_QUALIFIED= 1L << 35;

	/**
	 * Package names are post qualified.
	 * e.g. <code>java.util - MyProject/src</code>
	 */
	public final static long P_POST_QUALIFIED= 1L << 36;

	/**
	 * Package names are abbreviated if
	 * PreferenceConstants#APPEARANCE_ABBREVIATE_PACKAGE_NAMES is <code>true</code> and/or
	 * compressed if PreferenceConstants#APPEARANCE_COMPRESS_PACKAGE_NAMES is
	 * <code>true</code>.
	 */
	public final static long P_COMPRESSED= 1L << 37;

	/**
	 * Package Fragment Roots contain variable name if from a variable.
	 * e.g. <code>JRE_LIB - c:\java\lib\rt.jar</code>
	 */
	public final static long ROOT_VARIABLE= 1L << 40;

	/**
	 * Package Fragment Roots contain the project name if not an archive (prepended).
	 * e.g. <code>MyProject/src</code>
	 */
	public final static long ROOT_QUALIFIED= 1L << 41;

	/**
	 * Package Fragment Roots contain the project name if not an archive (appended).
	 * e.g. <code>src - MyProject</code>
	 */
	public final static long ROOT_POST_QUALIFIED= 1L << 42;

	/**
	 * Add root path to all elements except Package Fragment Roots and Java projects.
	 * e.g. <code>java.lang.Vector - C:\java\lib\rt.jar</code>
	 * Option only applies to getElementLabel
	 */
	public final static long APPEND_ROOT_PATH= 1L << 43;

	/**
	 * Add root path to all elements except Package Fragment Roots and Java projects.
	 * e.g. <code>C:\java\lib\rt.jar - java.lang.Vector</code>
	 * Option only applies to getElementLabel
	 */
	public final static long PREPEND_ROOT_PATH= 1L << 44;

	/**
	 * Post qualify referenced package fragment roots. For example
	 * <code>jdt.jar - org.eclipse.jdt.ui</code> if the jar is referenced
	 * from another project.
	 */
	public final static long REFERENCED_ROOT_POST_QUALIFIED= 1L << 45;

	/**
	 * Specifies to use the resolved information of a IType, IMethod or IField. See {@link IType#isResolved()}.
	 * If resolved information is available, types will be rendered with type parameters of the instantiated type.
	 * Resolved methods render with the parameter types of the method instance.
	 * <code>Vector&lt;String&gt;.get(String)</code>
	 */
	public final static long USE_RESOLVED= 1L << 48;

	/**
	 * Prepend first category (if any) to field.
	 */
	public final static long F_CATEGORY= 1L << 49;
	/**
	 * Prepend first category (if any) to method.
	 */
	public final static long M_CATEGORY= 1L << 50;
	/**
	 * Prepend first category (if any) to type.
	 */
	public final static long T_CATEGORY= 1L << 51;

	/**
	 * Method labels contain parameter annotations.
	 * E.g. <code>foo(@NonNull int)</code>.
	 * This flag is only valid if {@link #M_PARAMETER_NAMES} or {@link #M_PARAMETER_TYPES} is also set.
	 * @since 3.8
	 */
	public final static long M_PARAMETER_ANNOTATIONS= 1L << 52;

	/**
	 * Prepend first category (if any) to module.
	 *
	 * @deprecated Deprecated due to conflict with {@link #M_PARAMETER_ANNOTATIONS}. Use
	 *             {@link #MOD_CATEGORY2} instead
	 */
	@Deprecated
	public final static long MOD_CATEGORY= 1L << 52;

	/**
	 * Prepend first category (if any) to module.
	 */
	public final static long MOD_CATEGORY2= 1L << 53;

	/**
	 * Specifies to apply color styles to labels. This flag only applies to methods taking or returning a StyledString.
	 */
	public final static long COLORIZE= 1L << 55;

	/**
	 * Show category for all elements.
	 *
	 * @deprecated Use {@link #ALL_CATEGORY2} instead
	 */
	@Deprecated
	public final static long ALL_CATEGORY= F_CATEGORY | M_CATEGORY | T_CATEGORY | MOD_CATEGORY;

	/**
	 * Show category for all elements.
	 */
	public final static long ALL_CATEGORY2= F_CATEGORY | M_CATEGORY | T_CATEGORY | MOD_CATEGORY2;

	/**
	 * Qualify all elements
	 */
	public final static long ALL_FULLY_QUALIFIED= F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED | P_QUALIFIED | ROOT_QUALIFIED;

	/**
	 * Post qualify all elements
	 */
	public final static long ALL_POST_QUALIFIED= F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | TP_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED | P_POST_QUALIFIED | ROOT_POST_QUALIFIED;

	/**
	 *  Default options (M_PARAMETER_TYPES,  M_APP_TYPE_PARAMETERS & T_TYPE_PARAMETERS enabled)
	 */
	public final static long ALL_DEFAULT= M_PARAMETER_TYPES | M_APP_TYPE_PARAMETERS | T_TYPE_PARAMETERS;

	/**
	 *  Default qualify options (All except Root and Package)
	 */
	public final static long DEFAULT_QUALIFIED= F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED;

	/**
	 *  Default post qualify options (All except Root and Package)
	 */
	public final static long DEFAULT_POST_QUALIFIED= F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | TP_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED;

	/**
	 * User-readable string for separating post qualified names (e.g. " - ").
	 */
	public final static String CONCAT_STRING= JavaElementLabelsMessages.JavaElementLabels_concat_string;
	/**
	 * User-readable string for separating list items (e.g. ", ").
	 */
	public final static String COMMA_STRING= JavaElementLabelsMessages.JavaElementLabels_comma_string;
	/**
	 * User-readable string for separating the return type (e.g. " : ").
	 */
	public final static String DECL_STRING= JavaElementLabelsMessages.JavaElementLabels_declseparator_string;
	/**
	 * User-readable string for concatenating categories (e.g. " ").
	 * @since 3.5
	 */
	public final static String CATEGORY_SEPARATOR_STRING= JavaElementLabelsMessages.JavaElementLabels_category_separator_string;
	/**
	 * User-readable string for ellipsis ("...").
	 */
	public final static String ELLIPSIS_STRING= "..."; //$NON-NLS-1$

	/**
	 * User-readable string for the default package name (e.g. "(default package)").
	 */
	public final static String DEFAULT_PACKAGE= JavaElementLabelsMessages.JavaElementLabels_default_package;

	private JavaElementLabelsCore() {
	}

	/**
	 * Returns the label of the given object. The object must be of type {@link IJavaElement} or adapt to IWorkbenchAdapter.
	 * If the element type is not known, the empty string is returned.
	 * The returned label is BiDi-processed with {@link TextProcessor#process(String, String)}.
	 *
	 * @param obj object to get the label for
	 * @param flags the rendering flags
	 * @return the label or the empty string if the object type is not supported
	 */
	public static String getTextLabel(Object obj, long flags) {
		if (obj instanceof IJavaElement) {
			return getElementLabel((IJavaElement) obj, flags);

		} else if (obj instanceof IResource) {
			return BasicElementLabels.getResourceName((IResource) obj);

		} else if (obj instanceof IStorage) {
			return BasicElementLabels.getResourceName(((IStorage) obj).getName());

		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 *
	 * @param element the element to render
	 * @param flags the rendering flags
	 * @return the label of the Java element
	 */
	public static String getElementLabel(IJavaElement element, long flags) {
		StringBuffer result= new StringBuffer();
		getElementLabel(element, flags, result);
		return org.eclipse.jdt.internal.core.manipulation.util.Strings.markJavaElementLabelLTR(result.toString());
	}

	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 *
	 * @param element the element to render
	 * @param flags the rendering flags
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getElementLabel(IJavaElement element, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendElementLabel(element, flags);
	}

	/**
	 * Appends the label for a method to a {@link StringBuffer}. Considers the M_* flags.
	 *
	 * @param method the element to render
	 * @param flags the rendering flags. Flags with names starting with 'M_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getMethodLabel(IMethod method, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendMethodLabel(method, flags);
	}

	/**
	 * Appends the label for a field to a {@link StringBuffer}. Considers the F_* flags.
	 *
	 * @param field the element to render
	 * @param flags the rendering flags. Flags with names starting with 'F_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getFieldLabel(IField field, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendFieldLabel(field, flags);
	}

	/**
	 * Appends the label for a local variable to a {@link StringBuffer}.
	 *
	 * @param localVariable the element to render
	 * @param flags the rendering flags. Flags with names starting with 'F_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getLocalVariableLabel(ILocalVariable localVariable, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendLocalVariableLabel(localVariable, flags);
	}

	/**
	 * Appends the label for a initializer to a {@link StringBuffer}. Considers the I_* flags.
	 *
	 * @param initializer the element to render
	 * @param flags the rendering flags. Flags with names starting with 'I_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getInitializerLabel(IInitializer initializer, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendInitializerLabel(initializer, flags);
	}

	/**
	 * Appends the label for a type to a {@link StringBuffer}. Considers the T_* flags.
	 *
	 * @param type the element to render
	 * @param flags the rendering flags. Flags with names starting with 'T_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getTypeLabel(IType type, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendTypeLabel(type, flags);
	}

	/**
	 * Appends the label for a type parameter to a {@link StringBuffer}. Considers the TP_* flags.
	 *
	 * @param typeParameter the element to render
	 * @param flags the rendering flags. Flags with names starting with 'TP_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 *
	 * @since 3.5
	 */
	public static void getTypeParameterLabel(ITypeParameter typeParameter, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendTypeParameterLabel(typeParameter, flags);
	}

	/**
	 * Appends the label for a import container, import or package declaration to a {@link StringBuffer}. Considers the D_* flags.
	 *
	 * @param declaration the element to render
	 * @param flags the rendering flags. Flags with names starting with 'D_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getDeclarationLabel(IJavaElement declaration, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendDeclarationLabel(declaration, flags);
	}

	/**
	 * Appends the label for a class file to a {@link StringBuffer}. Considers the CF_* flags.
	 *
	 * @param classFile the element to render
	 * @param flags the rendering flags. Flags with names starting with 'CF_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getClassFileLabel(IClassFile classFile, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendClassFileLabel(classFile, flags);
	}

	/**
	 * Appends the label for a compilation unit to a {@link StringBuffer}. Considers the CU_* flags.
	 *
	 * @param cu the element to render
	 * @param flags the rendering flags. Flags with names starting with 'CU_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getCompilationUnitLabel(ICompilationUnit cu, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendCompilationUnitLabel(cu, flags);
	}

	/**
	 * Appends the label for a package fragment to a {@link StringBuffer}. Considers the P_* flags.
	 *
	 * @param pack the element to render
	 * @param flags the rendering flags. Flags with names starting with P_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getPackageFragmentLabel(IPackageFragment pack, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendPackageFragmentLabel(pack, flags);
	}

	/**
	 * Appends the label for a package fragment root to a {@link StringBuffer}. Considers the ROOT_* flags.
	 *
	 * @param root the element to render
	 * @param flags the rendering flags. Flags with names starting with ROOT_' are considered.
	 * @param buffer the buffer to append the resulting label to
	 */
	public static void getPackageFragmentRootLabel(IPackageFragmentRoot root, long flags, StringBuffer buffer) {
		new JavaElementLabelComposerCore(buffer).appendPackageFragmentRootLabel(root, flags);
	}

	/**
	 * Returns the label of a classpath container.
	 * The returned label is BiDi-processed with {@link TextProcessor#process(String, String)}.
	 *
	 * @param containerPath the path of the container
	 * @param project the project the container is resolved in
	 * @return the label of the classpath container
	 * @throws JavaModelException when resolving of the container failed
	 */
	public static String getContainerEntryLabel(IPath containerPath, IJavaProject project) throws JavaModelException {
		IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, project);
		if (container != null) {
			return org.eclipse.jdt.internal.core.manipulation.util.Strings.markLTR(container.getDescription());
		}
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
		if (initializer != null) {
			return org.eclipse.jdt.internal.core.manipulation.util.Strings.markLTR(initializer.getDescription(containerPath, project));
		}
		return BasicElementLabels.getPathLabel(containerPath, false);
	}
}
