/*
 * Copyright 2014 Blazebit.
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
package com.blazebit.persistence.impl.expression;

import com.blazebit.persistence.impl.predicate.*;

/**
 *
 * @author Christian Beikov
 * @author Moritz Becker
 * @since 1.0
 */
public interface Expression {

    public static interface Visitor {

        // Expressions
        public void visit(PathExpression expression);

        public void visit(PropertyExpression expression);

        public void visit(ParameterExpression expression);

        public void visit(ArrayExpression expression);

        public void visit(CompositeExpression expression);

        public void visit(NullExpression expression);

        public void visit(FooExpression expression);

        public void visit(SubqueryExpression expression);

        public void visit(FunctionExpression expression);

        public void visit(TypeFunctionExpression expression);

        public void visit(WhenClauseExpression expression);

        public void visit(GeneralCaseExpression expression);

        public void visit(SimpleCaseExpression expression);

        public void visit(ArithmeticExpression expression);

        public void visit(ArithmeticFactor expression);

        public void visit(NumericLiteral expression);

        public void visit(StringLiteral expression);

        public void visit(DateLiteral expression);

        public void visit(TimeLiteral expression);

        public void visit(TimestampLiteral expression);

        public void visit(EnumLiteral expression);

        public void visit(EntityLiteral expression);

        // Predicates
        public void visit(EqPredicate predicate);

        public void visit(IsNullPredicate predicate);

        public void visit(IsEmptyPredicate predicate);

        public void visit(MemberOfPredicate predicate);

        public void visit(LikePredicate predicate);

        public void visit(BetweenPredicate predicate);

        public void visit(InPredicate predicate);

        public void visit(GtPredicate predicate);

        public void visit(GePredicate predicate);

        public void visit(LtPredicate predicate);

        public void visit(LePredicate predicate);

        public void visit(ExistsPredicate predicate);

        public void visit(BooleanLiteral predicate);

        public void visit(CompoundPredicate predicate);

    }

    public static interface ResultVisitor<T> {

        // Expressions
        public T visit(PathExpression expression);

        public T visit(PropertyExpression expression);

        public T visit(ParameterExpression expression);

        public T visit(ArrayExpression expression);

        public T visit(CompositeExpression expression);

        public T visit(NullExpression expression);

        public T visit(FooExpression expression);

        public T visit(SubqueryExpression expression);

        public T visit(FunctionExpression expression);

        public T visit(TypeFunctionExpression expression);

        public T visit(WhenClauseExpression expression);

        public T visit(GeneralCaseExpression expression);

        public T visit(SimpleCaseExpression expression);

        public T visit(ArithmeticExpression expression);

        public T visit(ArithmeticFactor expression);

        public T visit(NumericLiteral expression);

        public T visit(StringLiteral expression);

        public T visit(DateLiteral expression);

        public T visit(TimeLiteral expression);

        public T visit(TimestampLiteral expression);

        public T visit(EnumLiteral expression);

        public T visit(EntityLiteral expression);

        // Predicates
        public T visit(EqPredicate predicate);

        public T visit(IsNullPredicate predicate);

        public T visit(IsEmptyPredicate predicate);

        public T visit(MemberOfPredicate predicate);

        public T visit(LikePredicate predicate);

        public T visit(BetweenPredicate predicate);

        public T visit(InPredicate predicate);

        public T visit(GtPredicate predicate);

        public T visit(GePredicate predicate);

        public T visit(LtPredicate predicate);

        public T visit(LePredicate predicate);

        public T visit(ExistsPredicate predicate);

        public T visit(BooleanLiteral predicate);

        public T visit(CompoundPredicate predicate);
    }

    public Expression clone();

    /**
     * The expression tree is traversed in pre-order.
     *
     * @param visitor
     */
    public void accept(Visitor visitor);

    public <T> T accept(ResultVisitor<T> visitor);

    /**
     * Returns the trimmed original string representation of the expression.
     *
     * @return The string representation of the expression
     */
    @Override
    public String toString();
}
