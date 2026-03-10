/*
 * Copyright 2021 - 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.sbm.java.impl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveAnnotationAttributeRecipe extends Recipe {

    String annotationType;
    String attributeName;

    @Override
    public @NotNull String getDisplayName() {
        return "Remove annotation attribute";
    }

    @Override
    public @NotNull String getDescription() {
        return "Removes an attribute from a matching annotation.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.@NotNull Annotation visitAnnotation(J.@NotNull Annotation annotation, @NotNull ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);

                if (!matchesAnnotationType(a, annotationType)) {
                    return a;
                }

                List<Expression> args = a.getArguments();
                if (args == null || args.isEmpty()) {
                    return a;
                }

                List<Expression> filtered = new ArrayList<>();
                for (Expression expr : args) {
                    if (!isTargetAttribute(expr, attributeName)) {
                        filtered.add(expr);
                    }
                }

                if (filtered.size() == args.size()) {
                    return a;
                }

                return a.withArguments(filtered.isEmpty() ? null : filtered);
            }

            private boolean matchesAnnotationType(J.Annotation annotation, String expectedFqn) {
                var type = TypeUtils.asFullyQualified(annotation.getAnnotationType().getType());
                return type != null && expectedFqn.equals(type.getFullyQualifiedName());
            }

            private boolean isTargetAttribute(Expression expr, String attributeName) {
                if (expr instanceof J.Assignment assignment) {
                    if (assignment.getVariable() instanceof J.Identifier ident) {
                        return attributeName.equals(ident.getSimpleName());
                    }
                    return false;
                }

                return "value".equals(attributeName);
            }
        };
    }
}
