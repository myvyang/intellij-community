/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.transformations;

import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.impl.light.LightPsiClassBuilder;
import com.intellij.psi.util.MethodSignature;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GrClassImplUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.intellij.psi.util.MethodSignatureUtil.METHOD_PARAMETERS_ERASURE_EQUALITY;
import static org.jetbrains.plugins.groovy.lang.psi.util.GrClassImplUtil.expandReflectedMethods;


public class TransformationContextImpl implements TransformationContext {

  private final @NotNull GrTypeDefinition myCodeClass;
  private final Set<MethodSignature> mySignatures = new THashSet<>(METHOD_PARAMETERS_ERASURE_EQUALITY);
  private final Collection<PsiMethod> myMethods = ContainerUtil.newArrayList();
  private final Collection<GrField> myFields = ContainerUtil.newArrayList();
  private final Collection<PsiClass> myInnerClasses = ContainerUtil.newArrayList();
  private final List<PsiClassType> myImplementsTypes = ContainerUtil.newArrayList();
  private final List<PsiClassType> myExtendsTypes = ContainerUtil.newArrayList();

  public TransformationContextImpl(@NotNull GrTypeDefinition codeClass) {
    myCodeClass = codeClass;
    ContainerUtil.addAll(myFields, codeClass.getCodeFields());
    for (GrMethod grMethod : codeClass.getCodeMethods()) {
      for (PsiMethod method : expandReflectedMethods(grMethod)) {
        mySignatures.add(method.getSignature(PsiSubstitutor.EMPTY));
        myMethods.add(method);
      }
    }
    ContainerUtil.addAll(myInnerClasses, codeClass.getCodeInnerClasses());
    ContainerUtil.addAll(myImplementsTypes, GrClassImplUtil.getReferenceListTypes(codeClass.getImplementsClause()));
    ContainerUtil.addAll(myExtendsTypes, GrClassImplUtil.getReferenceListTypes(codeClass.getExtendsClause()));
  }

  @Override
  @NotNull
  public GrTypeDefinition getCodeClass() {
    return myCodeClass;
  }

  @Override
  @NotNull
  public Collection<GrField> getFields() {
    return myFields;
  }

  @NotNull
  @Override
  public Collection<PsiMethod> getMethods() {
    return myMethods;
  }

  @Override
  @NotNull
  public Collection<PsiClass> getInnerClasses() {
    return myInnerClasses;
  }

  @Override
  @NotNull
  public List<PsiClassType> getImplementsTypes() {
    return myImplementsTypes;
  }

  @Override
  @SuppressWarnings("unused")
  @NotNull
  public List<PsiClassType> getExtendsTypes() {
    return myExtendsTypes;
  }

  @Override
  @NotNull
  public PsiManager getManager() {
    return getCodeClass().getManager();
  }

  @NotNull
  @Override
  public String getClassName() {
    return ObjectUtils.notNull(myCodeClass.getName());
  }

  @Override
  @Nullable
  public PsiClass getSuperClass() {
    return GrClassImplUtil.getSuperClass(getCodeClass(), getExtendsListTypesArray());
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation(@NotNull String fqn) {
    return PsiImplUtil.getAnnotation(getCodeClass(), fqn);
  }

  @NotNull
  @Override
  public Collection<PsiMethod> findMethodsByName(@NotNull String name, boolean checkBases) {
    List<PsiMethod> methods = ContainerUtil.filter(myMethods, m -> name.equals(m.getName()));
    if (checkBases) {
      PsiClass aClass = getSuperClass();
      if (aClass != null) ContainerUtil.addAll(ContainerUtil.newArrayList(methods), aClass.findMethodsByName(name, true));
    }
    return methods;
  }

  private void doAddMethod(@NotNull PsiMethod method) {
    if (method instanceof GrLightMethodBuilder) {
      ((GrLightMethodBuilder)method).setContainingClass(myCodeClass);
    }
    else if (method instanceof LightMethodBuilder) {
      ((LightMethodBuilder)method).setContainingClass(myCodeClass);
    }
    MethodSignature signature = method.getSignature(PsiSubstitutor.EMPTY);
    if (mySignatures.add(signature)) {
      myMethods.add(method);
    }
  }

  @Override
  public void addMethod(@NotNull PsiMethod method) {
    for (PsiMethod expanded : expandReflectedMethods(method)) {
      doAddMethod(expanded);
    }
  }

  @Override
  public void addMethods(@NotNull PsiMethod[] methods) {
    for (PsiMethod method : methods) {
      addMethod(method);
    }
  }

  @Override
  public void addMethods(@NotNull Collection<? extends PsiMethod> methods) {
    for (PsiMethod method : methods) {
      addMethod(method);
    }
  }

  @Override
  public void removeMethod(@NotNull PsiMethod method) {
    for (PsiMethod expanded : expandReflectedMethods(method)) {
      MethodSignature signature = expanded.getSignature(PsiSubstitutor.EMPTY);
      mySignatures.remove(signature);
      myMethods.removeIf(m -> METHOD_PARAMETERS_ERASURE_EQUALITY.equals(signature, m.getSignature(PsiSubstitutor.EMPTY)));
    }
  }

  @Override
  public void addField(@NotNull GrField field) {
    if (field instanceof GrLightField) {
      ((GrLightField)field).setContainingClass(getCodeClass());
    }
    myFields.add(field);
  }

  @Override
  public void addInnerClass(@NotNull PsiClass innerClass) {
    if (innerClass instanceof LightPsiClassBuilder) {
      ((LightPsiClassBuilder)innerClass).setContainingClass(getCodeClass());
    }
    myInnerClasses.add(innerClass);
  }

  @Override
  public void addInterface(@NotNull PsiClassType type) {
    if (getCodeClass().isInterface()) {
      myExtendsTypes.add(type);
    }
    else {
      myImplementsTypes.add(type);
    }
  }

  @NotNull
  private GrField[] getFieldsArray() {
    return getFields().toArray(GrField.EMPTY_ARRAY);
  }

  @NotNull
  private PsiMethod[] getMethodsArray() {
    return getMethods().toArray(PsiMethod.EMPTY_ARRAY);
  }

  @NotNull
  private PsiClass[] getInnerClassesArray() {
    return getInnerClasses().toArray(PsiClass.EMPTY_ARRAY);
  }

  @NotNull
  private PsiClassType[] getImplementsListTypesArray() {
    return getImplementsTypes().toArray(PsiClassType.EMPTY_ARRAY);
  }

  @NotNull
  private PsiClassType[] getExtendsListTypesArray() {
    return getExtendsTypes().toArray(PsiClassType.EMPTY_ARRAY);
  }

  @NotNull
  TransformationResult getTransformationResult() {
    return new TransformationResult(
      getMethodsArray(),
      getFieldsArray(),
      getInnerClassesArray(),
      getImplementsListTypesArray(),
      getExtendsListTypesArray()
    );
  }
}
