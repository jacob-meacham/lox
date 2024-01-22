package lox.parser

import lox.Environment

// Generated by generate_ast ast 2024-01-21 21:32:19

interface Stmt { 
    fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R
}

interface StmtVisitor<R> {
    fun visitExpressionStatement(stmt: ExpressionStatement, environment: Environment): R
	fun visitForStatement(stmt: ForStatement, environment: Environment): R
	fun visitVarStatement(stmt: VarStatement, environment: Environment): R
	fun visitFunctionStatement(stmt: FunctionStatement, environment: Environment): R
	fun visitBreakStatement(stmt: BreakStatement, environment: Environment): R
	fun visitContinueStatement(stmt: ContinueStatement, environment: Environment): R
	fun visitReturnStatement(stmt: ReturnStatement, environment: Environment): R
}

class ExpressionStatement(val expression: Expr) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R {
        return visitor.visitExpressionStatement(this, environment)
    }
}

class ForStatement(val loopVariable: Token, val loopOver: Expr, val block: Block) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R {
        return visitor.visitForStatement(this, environment)
    }
}

class VarStatement(val name: Token, val initializer: Expr?) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R {
        return visitor.visitVarStatement(this, environment)
    }
}

class FunctionStatement(val name: Token, val params: List<Token>, val body: Block) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R {
        return visitor.visitFunctionStatement(this, environment)
    }
}

class BreakStatement(val keyword: Token) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R {
        return visitor.visitBreakStatement(this, environment)
    }
}

class ContinueStatement(val keyword: Token) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R {
        return visitor.visitContinueStatement(this, environment)
    }
}

class ReturnStatement(val keyword: Token, val value: Expr?) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>, environment: Environment): R {
        return visitor.visitReturnStatement(this, environment)
    }
}
