package lox.parser

// Generated by generate_ast ast 2024-01-20 19:12:20

interface Stmt { 
    fun<R> accept(visitor: StmtVisitor<R>): R
}

interface StmtVisitor<R> {
    fun visitExpressionStatement(stmt: ExpressionStatement): R
	fun visitVarStatement(stmt: VarStatement): R
}

class ExpressionStatement(val expression: Expr) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>): R {
        return visitor.visitExpressionStatement(this)
    }
}

class VarStatement(val name: Token, val initializer: Expr?) : Stmt {
    override fun<R> accept(visitor: StmtVisitor<R>): R {
        return visitor.visitVarStatement(this)
    }
}
