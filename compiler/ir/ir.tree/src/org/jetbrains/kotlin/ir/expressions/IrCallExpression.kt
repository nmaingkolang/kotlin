/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrCallExpression : IrMemberAccessExpression {
    val superQualifier: ClassDescriptor?
    val operator: IrOperator?
    override val descriptor: CallableDescriptor

    fun getArgument(index: Int): IrExpression?
    fun putArgument(index: Int, valueArgument: IrExpression?)
    fun removeArgument(index: Int)
}

class IrCallExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val descriptor: CallableDescriptor,
        isSafe: Boolean,
        override val operator: IrOperator?,
        override val superQualifier: ClassDescriptor? = null
) : IrMemberAccessExpressionBase(startOffset, endOffset, type, isSafe), IrCallExpression {
    private val argumentsByParameterIndex =
            arrayOfNulls<IrExpression>(descriptor.valueParameters.size)

    override fun getArgument(index: Int): IrExpression? =
            argumentsByParameterIndex[index]

    override fun putArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= argumentsByParameterIndex.size) throw AssertionError("$this: No such argument slot: $index")
        valueArgument?.assertDetached()
        argumentsByParameterIndex[index]?.detach()
        argumentsByParameterIndex[index] = valueArgument
        valueArgument?.setTreeLocation(this, index)
    }

    override fun removeArgument(index: Int) {
        argumentsByParameterIndex[index]?.detach()
        argumentsByParameterIndex[index] = null
    }

    override fun getChild(slot: Int): IrElement? =
            if (0 <= slot)
                argumentsByParameterIndex.getOrNull(slot)
            else
                super.getChild(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        if (0 <= slot)
            putArgument(slot, newChild.assertCast())
        else
            super.replaceChild(slot, newChild)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitCallExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super.acceptChildren(visitor, data)
        argumentsByParameterIndex.forEach { it?.accept(visitor, data) }
    }
}