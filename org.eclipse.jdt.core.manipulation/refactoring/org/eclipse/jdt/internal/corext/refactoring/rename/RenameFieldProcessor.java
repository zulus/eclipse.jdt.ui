/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.CuCollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateCreator;
import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateFieldCreator;
import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateMethodCreator;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.ui.refactoring.IRefactoringProcessorIdsCore;
import org.eclipse.jdt.ui.refactoring.IRefactoringSaveModes;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

public class RenameFieldProcessor extends JavaRenameProcessor implements IReferenceUpdating, ITextUpdating, IDelegateUpdating {

	protected static final String ATTRIBUTE_TEXTUAL_MATCHES= "textual"; //$NON-NLS-1$
	private static final String ATTRIBUTE_RENAME_GETTER= "getter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_RENAME_SETTER= "setter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DELEGATE= "delegate"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DEPRECATE= "deprecate"; //$NON-NLS-1$
	private static final GroupCategorySet CATEGORY_LOCAL_RENAME= new GroupCategorySet(new GroupCategory("org.eclipse.jdt.internal.corext.refactoring.rename.renameType.local", RefactoringCoreMessages.RenameTypeProcessor_changeCategory_local_variables, RefactoringCoreMessages.RenameTypeProcessor_changeCategory_local_variables_description)); //$NON-NLS-1$

	protected IField fField;
	/* Record Related Fields */
	private boolean fIsRecordComponent= false;
	private RenameLocalVariableProcessor fRenameLocalVariableProcessor;
	private ILocalVariable fLocalVariable;
	private boolean fIsCompactConstructor;
	/* Record Related Fields End*/
	private SearchResultGroup[] fReferences;
	private TextChangeManager fChangeManager;
	protected boolean fUpdateReferences;
	protected boolean fUpdateTextualMatches;
	private boolean fRenameGetter;
	private boolean fRenameSetter;
	private boolean fIsComposite;
	private GroupCategorySet fCategorySet;
	private boolean fDelegateUpdating;
	private boolean fDelegateDeprecation;
	private CompilationUnit fCompUnit;

	/**
	 * Creates a new rename field processor.
	 * @param field the field, or <code>null</code> if invoked by scripting
	 */
	public RenameFieldProcessor(IField field) {
		this(field, new TextChangeManager(true), null);
		fIsComposite= false;
	}

	/**
	 * Creates a new rename enum const processor.
	 *
	 * @param arguments
	 *            the arguments
	 *
	 * @param status
	 *            the status
	 */
	public RenameFieldProcessor(JavaRefactoringArguments arguments, RefactoringStatus status) {
		this(null);
		RefactoringStatus initializeStatus= initialize(arguments);
		status.merge(initializeStatus);
	}

	/**
	 * Creates a new rename field processor.
	 * <p>
	 * This constructor is only used by <code>RenameTypeProcessor</code>.
	 * </p>
	 * @param field the field
	 * @param manager the change manager
	 * @param categorySet the group category set
	 */
	RenameFieldProcessor(IField field, TextChangeManager manager, GroupCategorySet categorySet) {
		initialize(field);
		fChangeManager= manager;
		fCategorySet= categorySet;
		fDelegateUpdating= false;
		fDelegateDeprecation= true;
		fIsComposite= true;
		fIsRecordComponent= false;
	}

	private void initialize(IField field) {
		assignField(field);
		if (fField != null) {
			setNewElementName(fField.getElementName());
		}
		fUpdateReferences= true;
		fUpdateTextualMatches= false;

		fRenameGetter= false;
		fRenameSetter= false;
	}

	@Override
	public void setNewElementName(String newName) {
		super.setNewElementName(newName);
		setLocalVariableProcessor();
	}

	@Override
	public String getIdentifier() {
		return IRefactoringProcessorIdsCore.RENAME_FIELD_PROCESSOR;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTesterCore.isRenameFieldAvailable(fField);
	}

	@Override
	public String getProcessorName() {
		return RefactoringCoreMessages.RenameFieldRefactoring_name;
	}

	@Override
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fField);
	}

	public IField getField() {
		return fField;
	}

	@Override
	public Object[] getElements() {
		return new Object[] { fField};
	}

	@Override
	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		result.rename(fField, new RenameArguments(getNewElementName(), getUpdateReferences()));
		if (fRenameGetter) {
			IMethod getter= getGetter();
			if (getter != null) {
				result.rename(getter, new RenameArguments(getNewGetterName(), getUpdateReferences()));
			}
		}
		if (fRenameSetter) {
			IMethod setter= getSetter();
			if (setter != null) {
				result.rename(setter, new RenameArguments(getNewSetterName(), getUpdateReferences()));
			}
		}
		if (fIsRecordComponent) {
			IMethod accessor= getAccessor();
			if (accessor != null) {
				result.rename(accessor, new RenameArguments(getNewElementName(), getUpdateReferences()));
			}
			if (fLocalVariable != null) {
				result.rename(fLocalVariable, new RenameArguments(getNewElementName(), getUpdateReferences()));
			}
		}
		return result;
	}

	@Override
	protected IFile[] getChangedFiles() {
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}

	//---- IRenameProcessor -------------------------------------

	@Override
	public final String getCurrentElementName(){
		return fField.getElementName();
	}

	@Override
	public final String getCurrentElementQualifier(){
		return fField.getDeclaringType().getFullyQualifiedName('.');
	}

	@Override
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkFieldName(newName, fField);

		if (isInstanceField(fField) && (!Checks.startsWithLowerCase(newName)))
			result.addWarning(fIsComposite
					? Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_should_start_lowercase2, new String[] { BasicElementLabels.getJavaElementName(newName), getDeclaringTypeLabel() })
					: RefactoringCoreMessages.RenameFieldRefactoring_should_start_lowercase);

		if (Checks.isAlreadyNamed(fField, newName))
			result.addError(fIsComposite
					? Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_another_name2, new String[] { BasicElementLabels.getJavaElementName(newName), getDeclaringTypeLabel() })
					: RefactoringCoreMessages.RenameFieldRefactoring_another_name,
					JavaStatusContext.create(fField));

		if (fField.getDeclaringType().getField(newName).exists())
			result.addError(fIsComposite
					? Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_field_already_defined2, new String[] { BasicElementLabels.getJavaElementName(newName), getDeclaringTypeLabel() })
					: RefactoringCoreMessages.RenameFieldRefactoring_field_already_defined,
					JavaStatusContext.create(fField.getDeclaringType().getField(newName)));
		return result;
	}

	private String getDeclaringTypeLabel() {
		return JavaElementLabelsCore.getElementLabel(fField.getDeclaringType(), JavaElementLabelsCore.ALL_DEFAULT);
	}

	@Override
	public Object getNewElement() {
		return fField.getDeclaringType().getField(getNewElementName());
	}

	//---- ITextUpdating2 ---------------------------------------------

	@Override
	public boolean canEnableTextUpdating() {
		return true;
	}

	@Override
	public boolean getUpdateTextualMatches() {
		return fUpdateTextualMatches;
	}

	@Override
	public void setUpdateTextualMatches(boolean update) {
		fUpdateTextualMatches= update;
	}

	//---- IReferenceUpdating -----------------------------------

	@Override
	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}

	@Override
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}

	//-- getter/setter --------------------------------------------------

	/**
	 * @return Error message or <code>null</code> if getter can be renamed.
	 * @throws CoreException should not happen
	 */
	public String canEnableGetterRenaming() throws CoreException{
		if (fField.getDeclaringType().isInterface())
			return getGetter() == null ? "": null; //$NON-NLS-1$

		IMethod getter= getGetter();
		if (getter == null)
			return ""; //$NON-NLS-1$
		final NullProgressMonitor monitor= new NullProgressMonitor();
		if (MethodChecks.isVirtual(getter)) {
			final ITypeHierarchy hierarchy= getter.getDeclaringType().newTypeHierarchy(monitor);
			if (MethodChecks.isDeclaredInInterface(getter, hierarchy, monitor) != null || MethodChecks.overridesAnotherMethod(getter, hierarchy) != null)
				return RefactoringCoreMessages.RenameFieldRefactoring_declared_in_supertype;
		}
		return null;
	}

	/**
	 * @return Error message or <code>null</code> if setter can be renamed.
	 * @throws CoreException should not happen
	 */
	public String canEnableSetterRenaming() throws CoreException{
		if (fField.getDeclaringType().isInterface())
			return getSetter() == null ? "": null; //$NON-NLS-1$

		IMethod setter= getSetter();
		if (setter == null)
			return "";	 //$NON-NLS-1$
		final NullProgressMonitor monitor= new NullProgressMonitor();
		if (MethodChecks.isVirtual(setter)) {
			final ITypeHierarchy hierarchy= setter.getDeclaringType().newTypeHierarchy(monitor);
			if (MethodChecks.isDeclaredInInterface(setter, hierarchy, monitor) != null || MethodChecks.overridesAnotherMethod(setter, hierarchy) != null)
				return RefactoringCoreMessages.RenameFieldRefactoring_declared_in_supertype;
		}
		return null;
	}

	public boolean getRenameGetter() {
		return fRenameGetter;
	}

	public void setRenameGetter(boolean renameGetter) {
		fRenameGetter= renameGetter;
	}

	public boolean getRenameSetter() {
		return fRenameSetter;
	}

	public void setRenameSetter(boolean renameSetter) {
		fRenameSetter= renameSetter;
	}

	public IMethod getGetter() throws CoreException {
		return GetterSetterUtil.getGetter(fField);
	}

	public IMethod getSetter() throws CoreException {
		return GetterSetterUtil.getSetter(fField);
	}

	private IMethod getAccessor() throws CoreException {
		return JavaModelUtil.findMethod(fField.getElementName(), new String[0], false, fField.getDeclaringType());
	}

	private void setLocalVariableProcessor() {
		if (fIsRecordComponent && fField != null) {
			IType parent= fField.getDeclaringType();
			try {
				if (parent != null && parent.isRecord()) {
					fCompUnit= SharedASTProviderCore.getAST(fField.getCompilationUnit(), SharedASTProviderCore.WAIT_YES, null);
					for (IJavaElement elem : parent.getChildren()) {
						if (elem instanceof IMethod
								&& ((IMethod) elem).isConstructor()) {
							IMethod method= (IMethod) elem;
							MethodDeclaration mDecl= ASTNodeSearchUtil.getMethodDeclarationNode(method, fCompUnit);
							if (mDecl != null) {
								IMethodBinding mBinding= mDecl.resolveBinding();
								if (mBinding != null && mBinding.isCanonicalConstructor()) {
									ILocalVariable[] localVars= method.getParameters();
									for (ILocalVariable lVar : localVars) {
										if (lVar.getElementName().equals(fField.getElementName())) {
											fLocalVariable= lVar;
											fIsCompactConstructor= mBinding.isCompactConstructor();
											fRenameLocalVariableProcessor= createLocalRenameProcessor(lVar, getNewElementName(), fCompUnit);
										}
									}
								}
							}
						}
					}
				}
			} catch (JavaModelException e) {
				//do nothing
			}
		}
	}

	public String getNewGetterName() throws CoreException {
		IMethod primaryGetterCandidate= JavaModelUtil.findMethod(GetterSetterUtil.getGetterName(fField, new String[0]), new String[0], false, fField.getDeclaringType());
		if (! JavaModelUtil.isBoolean(fField) || (primaryGetterCandidate != null && primaryGetterCandidate.exists()))
			return GetterSetterUtil.getGetterName(fField.getJavaProject(), getNewElementName(), fField.getFlags(), JavaModelUtil.isBoolean(fField), null);
		//bug 30906 describes why we need to look for other alternatives here
		return GetterSetterUtil.getGetterName(fField.getJavaProject(), getNewElementName(), fField.getFlags(), false, null);
	}

	public String getNewSetterName() throws CoreException {
		return GetterSetterUtil.getSetterName(fField.getJavaProject(), getNewElementName(), fField.getFlags(), JavaModelUtil.isBoolean(fField), null);
	}

	// ------------------- IDelegateUpdating ----------------------

	@Override
	public boolean canEnableDelegateUpdating() {
		return (getDelegateCount() > 0);
	}

	@Override
	public boolean getDelegateUpdating() {
		return fDelegateUpdating;
	}

	@Override
	public void setDelegateUpdating(boolean update) {
		fDelegateUpdating= update;
	}

	@Override
	public void setDeprecateDelegates(boolean deprecate) {
		fDelegateDeprecation= deprecate;
	}

	@Override
	public boolean getDeprecateDelegates() {
		return fDelegateDeprecation;
	}

	/**
	 * Returns the maximum number of delegates which can
	 * be created for the input elements of this refactoring.
	 *
	 * @return maximum number of delegates
	 */
	public int getDelegateCount() {
		int count= 0;
		try {
			if (RefactoringAvailabilityTesterCore.isDelegateCreationAvailable(getField()))
				count++;
			if (fRenameGetter && getGetter() != null)
				count++;
			if (fRenameSetter && getSetter() != null)
				count++;
			if (fIsRecordComponent && getAccessor() != null)
				count++;
		} catch (CoreException e) {
			// no-op
		}
		return count;
	}

	@Override
	public int getSaveMode() {
		return IRefactoringSaveModes.SAVE_REFACTORING;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		IField primary= (IField) fField.getPrimaryElement();
		if (primary == null || !primary.exists()) {
			String message= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_deleted, BasicElementLabels.getFileName(fField.getCompilationUnit()));
			return RefactoringStatus.createFatalErrorStatus(message);
		}
		assignField(primary);

		return Checks.checkIfCuBroken(fField);
	}

	@Override
	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			pm.beginTask("", 18); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_checking);
			RefactoringStatus result= new RefactoringStatus();
			result.merge(Checks.checkIfCuBroken(fField));
			if (result.hasFatalError())
				return result;
			result.merge(checkNewElementName(getNewElementName()));
			pm.worked(1);
			result.merge(checkEnclosingHierarchy());
			pm.worked(1);
			result.merge(checkNestedHierarchy(fField.getDeclaringType()));
			pm.worked(1);

			if (fUpdateReferences){
				pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_searching);
				fReferences= getReferences(new SubProgressMonitor(pm, 3), result);
				pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_checking);
			} else {
				fReferences= new SearchResultGroup[0];
				pm.worked(3);
			}

			if (fUpdateReferences)
				result.merge(analyzeAffectedCompilationUnits());
			else
				Checks.checkCompileErrorsInAffectedFile(result, fField.getResource());

			if (getGetter() != null && fRenameGetter){
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getGetter(), getNewGetterName()));
				result.merge(Checks.checkIfConstructorName(getGetter(), getNewGetterName(), fField.getDeclaringType().getElementName()));
			} else {
				pm.worked(1);
			}

			if (getSetter() != null && fRenameSetter){
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getSetter(), getNewSetterName()));
				result.merge(Checks.checkIfConstructorName(getSetter(), getNewSetterName(), fField.getDeclaringType().getElementName()));
			} else {
				pm.worked(1);
			}

			if (fIsRecordComponent){
				result.merge(checkRecordComponentAccessor(new SubProgressMonitor(pm, 1), getAccessor(), getNewElementName()));
				result.merge(Checks.checkIfConstructorName(getAccessor(), getNewElementName(), fField.getDeclaringType().getElementName()));
			} else {
				pm.worked(1);
			}

			if (fRenameLocalVariableProcessor != null && fIsRecordComponent) {
				result.merge(fRenameLocalVariableProcessor.checkInitialConditions(new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError())
					return result;
				result.merge(fRenameLocalVariableProcessor.checkFinalConditions(new SubProgressMonitor(pm, 1), context));
				if (result.hasFatalError())
					return result;
			}

			result.merge(createChanges(new SubProgressMonitor(pm, 10)));
			if (result.hasFatalError())
				return result;

			return result;
		} finally{
			pm.done();
		}
	}

	//----------
	private RefactoringStatus checkAccessor(IProgressMonitor pm, IMethod existingAccessor, String newAccessorName) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAccessorDeclarations(pm, existingAccessor));
		result.merge(checkNewAccessor(existingAccessor, newAccessorName));
		return result;
	}

	private RefactoringStatus checkRecordComponentAccessor(IProgressMonitor pm, IMethod existingAccessor, String newAccessorName) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		if (existingAccessor != null) {
			result.merge(checkAccessorDeclarations(pm, existingAccessor));
		}
		result.merge(checkNewRecordComponentAccessor(newAccessorName));
		return result;
	}

	private RefactoringStatus checkNewAccessor(IMethod existingAccessor, String newAccessorName) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		IMethod accessor= JavaModelUtil.findMethod(newAccessorName, existingAccessor.getParameterTypes(), false, fField.getDeclaringType());
		if (accessor == null || !accessor.exists())
			return null;

		String message= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_already_exists,
				new String[]{JavaElementUtil.createMethodSignature(accessor), BasicElementLabels.getJavaElementName(fField.getDeclaringType().getFullyQualifiedName('.'))});
		result.addError(message, JavaStatusContext.create(accessor));
		return result;
	}

	private RefactoringStatus checkNewRecordComponentAccessor(String newAccessorName) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		IMethod accessor= JavaModelUtil.findMethod(newAccessorName, new String[]{}, false, fField.getDeclaringType());
		if (accessor == null || !accessor.exists())
			return null;
		String message= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_recordromponent_accessor_method_already_exists,
				new String[]{JavaElementUtil.createMethodSignature(accessor), BasicElementLabels.getJavaElementName(fField.getDeclaringType().getFullyQualifiedName('.'))});
		result.addError(message, JavaStatusContext.create(accessor));
		return result;
	}

	private RefactoringStatus checkAccessorDeclarations(IProgressMonitor pm, IMethod existingAccessor) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		SearchPattern pattern= SearchPattern.createPattern(existingAccessor, IJavaSearchConstants.DECLARATIONS, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		if (pattern == null) {
			return result;
		}
		IJavaSearchScope scope= SearchEngine.createHierarchyScope(fField.getDeclaringType());
		SearchResultGroup[] groupDeclarations= RefactoringSearchEngine.search(pattern, scope, pm, result);
		Assert.isTrue(groupDeclarations.length > 0);
		if (groupDeclarations.length != 1){
			String message= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_overridden,
								JavaElementUtil.createMethodSignature(existingAccessor));
			result.addError(message);
		} else {
			SearchResultGroup group= groupDeclarations[0];
			Assert.isTrue(group.getSearchResults().length > 0);
			if (group.getSearchResults().length != 1){
				String message= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_overridden_or_overrides,
									JavaElementUtil.createMethodSignature(existingAccessor));
				result.addError(message);
			}
		}
		return result;
	}

	private static boolean isInstanceField(IField field) throws CoreException{
		if (JavaModelUtil.isInterfaceOrAnnotation(field.getDeclaringType()))
			return false;
		else
			return ! JdtFlags.isStatic(field);
	}

	private RefactoringStatus checkNestedHierarchy(IType type) throws CoreException {
		IType[] nestedTypes= type.getTypes();
		if (nestedTypes == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (IType nestedType : nestedTypes) {
			IField otherField= nestedType.getField(getNewElementName());
			if (otherField.exists()) {
				String msg= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_hiding, new String[]{BasicElementLabels.getJavaElementName(fField.getElementName()), BasicElementLabels.getJavaElementName(getNewElementName()), BasicElementLabels.getJavaElementName(nestedType.getFullyQualifiedName('.'))});
				result.addWarning(msg, JavaStatusContext.create(otherField));
			}
			result.merge(checkNestedHierarchy(nestedType));
		}
		return result;
	}

	private RefactoringStatus checkEnclosingHierarchy() {
		IType current= fField.getDeclaringType();
		if (Checks.isTopLevel(current))
			return null;
		RefactoringStatus result= new RefactoringStatus();
		while (current != null){
			IField otherField= current.getField(getNewElementName());
			if (otherField.exists()){
				String msg= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_hiding2,
				 	new String[]{ BasicElementLabels.getJavaElementName(getNewElementName()), BasicElementLabels.getJavaElementName(current.getFullyQualifiedName('.')), BasicElementLabels.getJavaElementName(otherField.getElementName())});
				result.addWarning(msg, JavaStatusContext.create(otherField));
			}
			current= current.getDeclaringType();
		}
		return result;
	}

	/*
	 * (non java-doc)
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits() throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		fReferences= Checks.excludeCompilationUnits(fReferences, result);
		if (result.hasFatalError())
			return result;

		result.merge(Checks.checkCompileErrorsInAffectedFiles(fReferences));
		return result;
	}

	private SearchPattern createSearchPattern(){
		return SearchPattern.createPattern(fField, IJavaSearchConstants.REFERENCES);
	}

	private IJavaSearchScope createRefactoringScope() throws CoreException{
		return RefactoringScopeFactory.create(fField, true, false);
	}

	private SearchResultGroup[] getReferences(IProgressMonitor pm, RefactoringStatus status) throws CoreException{
		String binaryRefsDescription= Messages.format(RefactoringCoreMessages.ReferencesInBinaryContext_ref_in_binaries_description , BasicElementLabels.getJavaElementName(getCurrentElementName()));
		ReferencesInBinaryContext binaryRefs= new ReferencesInBinaryContext(binaryRefsDescription);

		SearchPattern searchPattern= createSearchPattern();
		if (searchPattern == null) {
			return new SearchResultGroup[0];
		}
		SearchResultGroup[] result= RefactoringSearchEngine.search(searchPattern, createRefactoringScope(),
				new CuCollectingSearchRequestor(binaryRefs), pm, status);
		binaryRefs.addErrorIfNecessary(status);
		result= filterAccessorMethods(result, true);
		return result;
	}

	/**
	 * @param grouped the list that needs to be filtered
	 * @param filterOut if <code>true</code>, filters out the references to record component
	 *            accessor methods. If <code>false</code>, returns only the references to record
	 *            component accessor methods.
	 * @return the filtered list
	 */
	private SearchResultGroup[] filterAccessorMethods(SearchResultGroup[] grouped, boolean filterOut) {
		if (this.fIsRecordComponent) {
			List<SearchResultGroup> result= new ArrayList<>();
			for (SearchResultGroup g : grouped) {
				SearchMatch[] matches= g.getSearchResults();
				List<SearchMatch> newList= new ArrayList<>();
				for (SearchMatch match : matches) {
					if (match instanceof MethodReferenceMatch) {
						if (filterOut) {
							continue;
						}
						newList.add(match);
					} else if (filterOut) {
						newList.add(match);
					}
				}
				if (newList.size() != matches.length) {
					result.add(new SearchResultGroup(g.getResource(), newList.toArray(new SearchMatch[newList.size()])));
				} else {
					result.add(g);
				}
			}
			return result.toArray(new SearchResultGroup[result.size()]);
		} else {
			return grouped;
		}

	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.RenameFieldRefactoring_checking, 1);
			TextChange[] changes= fChangeManager.getAllChanges();
			RenameJavaElementDescriptor descriptor= createRefactoringDescriptor();
			return new DynamicValidationRefactoringChange(descriptor, getProcessorName(), changes);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Overridden by subclasses.
	 * @return return the refactoring descriptor for this refactoring
	 */
	protected RenameJavaElementDescriptor createRefactoringDescriptor() {
		String project= null;
		IJavaProject javaProject= fField.getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		int flags= JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE;
		try {
			if (!Flags.isPrivate(fField.getFlags()))
				flags|= RefactoringDescriptor.MULTI_CHANGE;
		} catch (JavaModelException exception) {
			JavaManipulationPlugin.log(exception);
		}
		final IType declaring= fField.getDeclaringType();
		try {
			if (declaring.isAnonymous() || declaring.isLocal())
				flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		} catch (JavaModelException exception) {
			JavaManipulationPlugin.log(exception);
		}
		final String description= Messages.format(RefactoringCoreMessages.RenameFieldRefactoring_descriptor_description_short, BasicElementLabels.getJavaElementName(fField.getElementName()));
		final String header= Messages.format(RefactoringCoreMessages.RenameFieldProcessor_descriptor_description, new String[] { BasicElementLabels.getJavaElementName(fField.getElementName()), JavaElementLabelsCore.getElementLabel(fField.getParent(), JavaElementLabelsCore.ALL_FULLY_QUALIFIED), getNewElementName()});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		if (fRenameGetter)
			comment.addSetting(RefactoringCoreMessages.RenameFieldRefactoring_setting_rename_getter);
		if (fRenameSetter)
			comment.addSetting(RefactoringCoreMessages.RenameFieldRefactoring_setting_rename_settter);
		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_FIELD);
		descriptor.setProject(project);
		descriptor.setDescription(description);
		descriptor.setComment(comment.asString());
		descriptor.setFlags(flags);
		descriptor.setJavaElement(fField);
		descriptor.setNewName(getNewElementName());
		descriptor.setUpdateReferences(fUpdateReferences);
		descriptor.setUpdateTextualOccurrences(fUpdateTextualMatches);
		descriptor.setRenameGetters(fRenameGetter);
		descriptor.setRenameSetters(fRenameSetter);
		descriptor.setKeepOriginal(fDelegateUpdating);
		descriptor.setDeprecateDelegate(fDelegateDeprecation);
		return descriptor;
	}

	private RefactoringStatus createChanges(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.RenameFieldRefactoring_checking, 10);
		RefactoringStatus result= new RefactoringStatus();
		if (!fIsComposite)
			fChangeManager.clear();

		// Delegate creation requires ASTRewrite which
		// creates a new change -> do this first.
		if (fDelegateUpdating)
			result.merge(addDelegates());

		addDeclarationUpdate();

		if (fUpdateReferences) {
			addReferenceUpdates(new SubProgressMonitor(pm, 1));
			result.merge(analyzeRenameChanges(new SubProgressMonitor(pm, 2)));
			if (result.hasFatalError())
				return result;
		} else {
			pm.worked(3);
		}

		if (getGetter() != null && fRenameGetter) {
			addGetterOccurrences(new SubProgressMonitor(pm, 1), result);
		} else {
			pm.worked(1);
		}

		if (getSetter() != null && fRenameSetter) {
			addSetterOccurrences(new SubProgressMonitor(pm, 1), result);
		} else {
			pm.worked(1);
		}

		if (fIsRecordComponent) {
			addAccessorOccurrences(new SubProgressMonitor(pm, 1), result);
			if (fRenameLocalVariableProcessor != null) {
				addLocalVariableOccurrences(getNewElementName(), result);
			}
		} else {
			pm.worked(1);
		}

		if (fUpdateTextualMatches) {
			addTextMatches(new SubProgressMonitor(pm, 5));
		} else {
			pm.worked(5);
		}
		pm.done();
		return result;
	}

	private void addDeclarationUpdate() throws CoreException {
		ISourceRange nameRange= fField.getNameRange();
		TextEdit textEdit= new ReplaceEdit(nameRange.getOffset(), nameRange.getLength(), getNewElementName());
		ICompilationUnit cu= fField.getCompilationUnit();
		String groupName= RefactoringCoreMessages.RenameFieldRefactoring_Update_field_declaration;
		addTextEdit(fChangeManager.get(cu), groupName, textEdit);
	}

	private RefactoringStatus addDelegates() throws JavaModelException, CoreException {

		RefactoringStatus status= new RefactoringStatus();
		CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fField.getCompilationUnit());
		rewrite.setResolveBindings(true);

		// add delegate for the field
		if (RefactoringAvailabilityTesterCore.isDelegateCreationAvailable(fField)) {
			FieldDeclaration fieldDeclaration= ASTNodeSearchUtil.getFieldDeclarationNode(fField, rewrite.getRoot());
			if (fieldDeclaration.fragments().size() > 1) {
				status.addWarning(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_field_delegate_more_than_one_fragment, BasicElementLabels.getJavaElementName(fField.getElementName())),
						JavaStatusContext.create(fField));
			} else if (((VariableDeclarationFragment) fieldDeclaration.fragments().get(0)).getInitializer() == null) {
				status.addWarning(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_field_delegate_no_initializer, BasicElementLabels.getJavaElementName(fField.getElementName())),
						JavaStatusContext.create(fField));
			} else {
				DelegateFieldCreator creator= new DelegateFieldCreator();
				creator.setDeclareDeprecated(fDelegateDeprecation);
				creator.setDeclaration(fieldDeclaration);
				creator.setNewElementName(getNewElementName());
				creator.setSourceRewrite(rewrite);
				creator.prepareDelegate();
				creator.createEdit();
			}
		}

		// add delegates for getter and setter methods
		// there may be getters even if the field is static final
		if (getGetter() != null && fRenameGetter)
			addMethodDelegate(getGetter(), getNewGetterName(), rewrite);
		if (getSetter() != null && fRenameSetter)
			addMethodDelegate(getSetter(), getNewSetterName(), rewrite);
		if (fIsRecordComponent && getAccessor() != null) {
			addMethodDelegate(getAccessor(), getNewElementName(), rewrite);
		}
		final CompilationUnitChange change= rewrite.createChange(true);
		if (change != null) {
			change.setKeepPreviewEdits(true);
			fChangeManager.manage(fField.getCompilationUnit(), change);
		}

		return status;
	}

	private void addMethodDelegate(IMethod getter, String newName, CompilationUnitRewrite rewrite) throws JavaModelException {
		MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(getter, rewrite.getRoot());
		DelegateCreator creator= new DelegateMethodCreator();
		creator.setDeclareDeprecated(fDelegateDeprecation);
		creator.setDeclaration(declaration);
		creator.setNewElementName(newName);
		creator.setSourceRewrite(rewrite);
		creator.prepareDelegate();
		creator.createEdit();
	}

	private void addTextEdit(TextChange change, String groupName, TextEdit textEdit) {
		if (fIsComposite)
			TextChangeCompatibility.addTextEdit(change, groupName, textEdit, fCategorySet);
		else
			TextChangeCompatibility.addTextEdit(change, groupName, textEdit);

	}

	private void addReferenceUpdates(IProgressMonitor pm) {
		pm.beginTask("", fReferences.length); //$NON-NLS-1$
		String editName= RefactoringCoreMessages.RenameFieldRefactoring_Update_field_reference;
		for (SearchResultGroup reference : fReferences) {
			ICompilationUnit cu= reference.getCompilationUnit();
			if (cu == null)
				continue;
			for (SearchMatch result : reference.getSearchResults()) {
				addTextEdit(fChangeManager.get(cu), editName, createTextChange(result));
			}
			pm.worked(1);
		}
	}

	private TextEdit createTextChange(SearchMatch match) {
		return new ReplaceEdit(match.getOffset(), match.getLength(), getNewElementName());
	}

	private void addGetterOccurrences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		addAccessorOccurrences(pm, getGetter(), RefactoringCoreMessages.RenameFieldRefactoring_Update_getter_occurrence, getNewGetterName(), status);
	}

	private void addSetterOccurrences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		addAccessorOccurrences(pm, getSetter(), RefactoringCoreMessages.RenameFieldRefactoring_Update_setter_occurrence, getNewSetterName(), status);
	}

	private void addAccessorOccurrences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		if (getAccessor() != null) {
			addAccessorOccurrences(pm, getAccessor(), RefactoringCoreMessages.RenameFieldRefactoring_Update_getter_occurrence, getNewElementName(), status);
		} else {
			addFieldAccessorOccurrences(pm, RefactoringCoreMessages.RenameFieldRefactoring_Update_getter_occurrence, getNewElementName(), status);
		}
	}

	private void addAccessorOccurrences(IProgressMonitor pm, IMethod accessor, String editName, String newAccessorName, RefactoringStatus status) throws CoreException {
		Assert.isTrue(accessor.exists());

		IJavaSearchScope scope= RefactoringScopeFactory.create(accessor);
		SearchPattern pattern= SearchPattern.createPattern(accessor, IJavaSearchConstants.ALL_OCCURRENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		if (pattern == null) {
			return;
		}
		SearchResultGroup[] groupedResults= RefactoringSearchEngine.search(
			pattern, scope, new MethodOccurenceCollector(accessor.getElementName()), pm, status);

		for (SearchResultGroup groupedResult : groupedResults) {
			ICompilationUnit cu= groupedResult.getCompilationUnit();
			if (cu == null)
				continue;
			SearchMatch[] results= groupedResult.getSearchResults();
			for (SearchMatch searchResult : results) {
				TextEdit edit= new ReplaceEdit(searchResult.getOffset(), searchResult.getLength(), newAccessorName);
				addTextEdit(fChangeManager.get(cu), editName, edit);
			}
		}
	}

	private void addLocalVariableOccurrences(String newName, RefactoringStatus status) throws CoreException {
		Assert.isTrue(this.fRenameLocalVariableProcessor != null);

		int current= 0;
		ICompilationUnit cu= fField.getCompilationUnit();
		RenameAnalyzeUtil.LocalAnalyzePackage[] analyzePackages= new RenameAnalyzeUtil.LocalAnalyzePackage[1];
		RenameAnalyzeUtil.LocalAnalyzePackage analyzePackage= fRenameLocalVariableProcessor.getLocalAnalyzePackage();
		analyzePackages[current]= analyzePackage;
		for (TextEdit occurenceEdit : analyzePackage.fOccurenceEdits) {
			addTextEdit(fChangeManager.get(cu), newName, occurenceEdit);
		}
		if (!fIsCompactConstructor) {
			status.merge(RenameAnalyzeUtil.analyzeLocalRenames(analyzePackages, fChangeManager.get(cu), fCompUnit, false));
		} else {
			status.merge(RenameAnalyzeUtil.analyzeCompactConstructorLocalRenames(analyzePackages, fChangeManager.get(cu), fCompUnit, false));
		}
	}


	private void addFieldAccessorOccurrences(IProgressMonitor pm, String editName, String newAccessorName, RefactoringStatus status) throws CoreException {
		Assert.isTrue(fField.exists());
		String fieldName= fField.getElementName();
		IJavaSearchScope scope= RefactoringScopeFactory.create(fField.getDeclaringType());

		String binaryRefsDescription= Messages.format(RefactoringCoreMessages.ReferencesInBinaryContext_ref_in_binaries_description , BasicElementLabels.getJavaElementName(getCurrentElementName()));
		ReferencesInBinaryContext binaryRefs= new ReferencesInBinaryContext(binaryRefsDescription);

		SearchPattern searchPattern= createSearchPattern();
		if (searchPattern == null) {
			return;
		}
		SearchResultGroup[] result= RefactoringSearchEngine.search(searchPattern, scope,
				new CuCollectingSearchRequestor(binaryRefs), pm, status);
		binaryRefs.addErrorIfNecessary(status);
		result= filterAccessorMethods(result, false);
		for (SearchResultGroup groupedResult : result) {
			ICompilationUnit cu= groupedResult.getCompilationUnit();
			if (cu == null)
				continue;
			SearchMatch[] results= groupedResult.getSearchResults();
			for (SearchMatch searchResult : results) {
				TextEdit edit= new ReplaceEdit(searchResult.getOffset(), fieldName.length(), newAccessorName);
				addTextEdit(fChangeManager.get(cu), editName, edit);
			}
		}
	}

	private void addTextMatches(IProgressMonitor pm) throws CoreException {
		TextMatchUpdater.perform(pm, createRefactoringScope(), this, fChangeManager, fReferences);
	}

	private void assignField(IField field) {
		fField= field;
		fIsRecordComponent= false;
		if (fField != null) {
			IType parent= fField.getDeclaringType();
			try {
				if (parent != null && parent.isRecord() && !Flags.isStatic(fField.getFlags())) {
					fIsRecordComponent= true;
				}
			} catch (JavaModelException e) {
				//do nothing
			}
		}
	}

	//----------------
	private RefactoringStatus analyzeRenameChanges(IProgressMonitor pm) throws CoreException {
		ICompilationUnit[] newWorkingCopies= null;
		WorkingCopyOwner newWCOwner= new WorkingCopyOwner() { /* must subclass */ };
		try {
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			SearchResultGroup[] oldReferences= fReferences;

			List<ICompilationUnit> compilationUnitsToModify= new ArrayList<>();
			if (fIsComposite) {
				// limited change set, no accessors.
				for (SearchResultGroup oldReference : oldReferences) {
					compilationUnitsToModify.add(oldReference.getCompilationUnit());
				}
				compilationUnitsToModify.add(fField.getCompilationUnit());
			} else {
				// include all cus, including accessors
				compilationUnitsToModify.addAll(Arrays.asList(fChangeManager.getAllCompilationUnits()));
			}

			newWorkingCopies= RenameAnalyzeUtil.createNewWorkingCopies(compilationUnitsToModify.toArray(new ICompilationUnit[compilationUnitsToModify.size()]),
					fChangeManager, newWCOwner, new SubProgressMonitor(pm, 1));

			SearchResultGroup[] newReferences= getNewReferences(new SubProgressMonitor(pm, 1), result, newWCOwner, newWorkingCopies);
			result.merge(RenameAnalyzeUtil.analyzeRenameChanges2(fChangeManager, oldReferences, newReferences, getNewElementName()));
			return result;
		} finally{
			pm.done();
			if (newWorkingCopies != null){
				for (ICompilationUnit newWorkingCopy : newWorkingCopies) {
					newWorkingCopy.discardWorkingCopy();
				}
			}
		}
	}

	private SearchResultGroup[] getNewReferences(IProgressMonitor pm, RefactoringStatus status, WorkingCopyOwner owner, ICompilationUnit[] newWorkingCopies) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		ICompilationUnit declaringCuWorkingCopy= RenameAnalyzeUtil.findWorkingCopyForCu(newWorkingCopies, fField.getCompilationUnit());
		if (declaringCuWorkingCopy == null)
			return new SearchResultGroup[0];

		IField field= getFieldInWorkingCopy(declaringCuWorkingCopy, getNewElementName());
		if (field == null || ! field.exists())
			return new SearchResultGroup[0];

		CollectingSearchRequestor requestor= null;
		if (fDelegateUpdating && RefactoringAvailabilityTesterCore.isDelegateCreationAvailable(getField())) {
			// There will be two new matches inside the delegate (the invocation
			// and the javadoc) which are OK and must not be reported.
			final IField oldField= getFieldInWorkingCopy(declaringCuWorkingCopy, getCurrentElementName());
			requestor= new CollectingSearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					if (!oldField.equals(match.getElement()))
						super.acceptSearchMatch(match);
				}
			};
		} else
			requestor= new CollectingSearchRequestor();

		SearchPattern newPattern= SearchPattern.createPattern(field, IJavaSearchConstants.REFERENCES);
		if (newPattern == null) {
			return new SearchResultGroup[0];
		}
		IJavaSearchScope scope= RefactoringScopeFactory.create(fField, true, true);
		return RefactoringSearchEngine.search(newPattern, owner, scope, requestor, new SubProgressMonitor(pm, 1), status);
	}

	private IField getFieldInWorkingCopy(ICompilationUnit newWorkingCopyOfDeclaringCu, String elementName) {
		IType type= fField.getDeclaringType();
		IType typeWc= (IType) JavaModelUtil.findInCompilationUnit(newWorkingCopyOfDeclaringCu, type);
		if (typeWc == null)
			return null;

		return typeWc.getField(elementName);
	}

	private RefactoringStatus initialize(JavaRefactoringArguments extended) {
		final String handle= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.FIELD)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(), IJavaRefactorings.RENAME_FIELD);
			else
				assignField((IField) element);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		final String name= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME);
		if (name != null && !"".equals(name)) //$NON-NLS-1$
			setNewElementName(name);
		else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME));
		final String references= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_REFERENCES);
		if (references != null) {
			fUpdateReferences= Boolean.parseBoolean(references);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_REFERENCES));
		final String matches= extended.getAttribute(ATTRIBUTE_TEXTUAL_MATCHES);
		if (matches != null) {
			fUpdateTextualMatches= Boolean.parseBoolean(matches);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_TEXTUAL_MATCHES));
		final String getters= extended.getAttribute(ATTRIBUTE_RENAME_GETTER);
		if (getters != null)
			fRenameGetter= Boolean.parseBoolean(getters);
		else
			fRenameGetter= false;
		final String setters= extended.getAttribute(ATTRIBUTE_RENAME_SETTER);
		if (setters != null)
			fRenameSetter= Boolean.parseBoolean(setters);
		else
			fRenameSetter= false;
		final String delegate= extended.getAttribute(ATTRIBUTE_DELEGATE);
		if (delegate != null) {
			fDelegateUpdating= Boolean.parseBoolean(delegate);
		} else
			fDelegateUpdating= false;
		final String deprecate= extended.getAttribute(ATTRIBUTE_DEPRECATE);
		if (deprecate != null) {
			fDelegateDeprecation= Boolean.parseBoolean(deprecate);
		} else
			fDelegateDeprecation= false;
		return new RefactoringStatus();
	}

	@Override
	public String getDelegateUpdatingTitle(boolean plural) {
		if (plural)
			return RefactoringCoreMessages.DelegateFieldCreator_keep_original_renamed_plural;
		else
			return RefactoringCoreMessages.DelegateFieldCreator_keep_original_renamed_singular;
	}

	private RenameLocalVariableProcessor createLocalRenameProcessor(final ILocalVariable local, final String newName, final CompilationUnit compilationUnit) {
		final RenameLocalVariableProcessor processor= new RenameLocalVariableProcessor(local, fChangeManager, compilationUnit, CATEGORY_LOCAL_RENAME);
		processor.setNewElementName(newName);
		processor.setUpdateReferences(getUpdateReferences());
		return processor;
	}
}
