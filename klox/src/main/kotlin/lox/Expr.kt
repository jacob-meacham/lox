package lox

// Generated by generate_ast ast 2024-01-16 00:47:27

interface Expr { 
    fun<R> accept(visitor: ExprVisitor<R>): R
}  

interface ExprVisitor<R> {
    fun visitBinary(expr: Binary): R
	fun visitGrouping(expr: Grouping): R
	fun visitLiteral(expr: Literal): R
	fun visitUnary(expr: Unary): R
	fun visitSubscription(expr: Subscription): R
	fun visitSlice(expr: Slice): R
}

class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr {
    override fun<R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitBinary(this)
    }
}

class Grouping(val expression: Expr) : Expr {
    override fun<R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitGrouping(this)
    }
}

class Literal(val value: Any?) : Expr {
    override fun<R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitLiteral(this)
    }
}

class Unary(val operator: Token, val right: Expr) : Expr {
    override fun<R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitUnary(this)
    }
}

class Subscription(val left: Expr, val index: Expr) : Expr {
    override fun<R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitSubscription(this)
    }
}

class Slice(val left: Expr, val start: Expr, val end: Expr) : Expr {
    override fun<R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitSlice(this)
    }
}
