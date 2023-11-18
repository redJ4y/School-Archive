use std::fmt::{Display, Error};

#[derive(PartialEq, Clone, Debug)]
pub enum Expression {
    Variable(char),
    Int(u32),
    True,
    False,
    LessThan(Box<Expression>, Box<Expression>),
    Equals(Box<Expression>, Box<Expression>),
    Add(Box<Expression>, Box<Expression>),
    Subtract(Box<Expression>, Box<Expression>),
    Multiply(Box<Expression>, Box<Expression>),
    Divide(Box<Expression>, Box<Expression>),
    And(Box<Expression>, Box<Expression>),
    Or(Box<Expression>, Box<Expression>),
    Not(Box<Expression>),
    Func(Box<Expression>, Box<Expression>), // Variable variant unenforced here
    Apply(Box<Expression>, Box<Expression>), // Func variant unenforced here
    If(Box<Expression>, Box<Expression>, Box<Expression>),
}

impl Display for Expression {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> Result<(), Error> {
        match self {
            Expression::Variable(x) => write!(f, "{}", x),
            Expression::Int(i) => write!(f, "{}", i),
            Expression::True => write!(f, "T"),
            Expression::False => write!(f, "F"),
            Expression::LessThan(e1, e2) => write!(f, "{} < {}", e1, e2),
            Expression::Equals(e1, e2) => write!(f, "{} = {}", e1, e2),
            Expression::Add(e1, e2) => write!(f, "{} + {}", e1, e2),
            Expression::Subtract(e1, e2) => write!(f, "{} - {}", e1, e2),
            Expression::Multiply(e1, e2) => write!(f, "{} * {}", e1, e2),
            Expression::Divide(e1, e2) => write!(f, "{} / {}", e1, e2),
            Expression::And(e1, e2) => write!(f, "{} & {}", e1, e2),
            Expression::Or(e1, e2) => write!(f, "{} | {}", e1, e2),
            Expression::Not(e) => write!(f, "!{}", e),
            Expression::Func(x, e) => write!(f, "func {} => {}", x, e),
            Expression::Apply(func, e) => write!(f, "{} ({})", func, e),
            Expression::If(cond, e1, e2) => write!(f, "if {} then {} else {}", cond, e1, e2),
        }
    }
}

impl Expression {
    pub fn eval(&self) -> Result<Expression, String> {
        match self {
            Expression::Variable(x) => Ok(Expression::Variable(*x)),
            Expression::Int(i) => Ok(Expression::Int(*i)),
            Expression::True => Ok(Expression::True),
            Expression::False => Ok(Expression::False),
            Expression::LessThan(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::Int(l), Expression::Int(r)) => {
                    if l < r {
                        Ok(Expression::True)
                    } else {
                        Ok(Expression::False)
                    }
                }
                _ => Err("Integer operation '<(e,e)' given non-integer".to_owned()),
            },
            Expression::Equals(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::Int(l), Expression::Int(r)) => {
                    if l == r {
                        Ok(Expression::True)
                    } else {
                        Ok(Expression::False)
                    }
                }
                _ => Err("Integer operation '=(e,e)' given non-integer".to_owned()),
            },
            Expression::Add(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::Int(l), Expression::Int(r)) => Ok(Expression::Int(l + r)),
                _ => Err("Integer operation '+(e,e)' given non-integer".to_owned()),
            },
            Expression::Subtract(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::Int(l), Expression::Int(r)) => Ok(Expression::Int(l - r)),
                _ => Err("Integer operation '-(e,e)' given non-integer".to_owned()),
            },
            Expression::Multiply(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::Int(l), Expression::Int(r)) => Ok(Expression::Int(l * r)),
                _ => Err("Integer operation '*(e,e)' given non-integer".to_owned()),
            },
            Expression::Divide(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::Int(l), Expression::Int(r)) => Ok(Expression::Int(l / r)),
                _ => Err("Integer operation '/(e,e)' given non-integer".to_owned()),
            },
            Expression::And(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::True, Expression::True) => Ok(Expression::True),
                (Expression::False, _) | (_, Expression::False) => Ok(Expression::False),
                _ => Err("Boolean operation '&(e,e)' given non-boolean".to_owned()),
            },
            Expression::Or(e1, e2) => match (e1.eval()?, e2.eval()?) {
                (Expression::True, _) | (_, Expression::True) => Ok(Expression::True),
                (Expression::False, Expression::False) => Ok(Expression::False),
                _ => Err("Boolean operation '|(e,e)' given non-boolean".to_owned()),
            },
            Expression::Not(e) => match e.eval()? {
                Expression::True => Ok(Expression::False),
                Expression::False => Ok(Expression::True),
                _ => Err("Boolean operation '!e' given non-boolean".to_owned()),
            },
            Expression::Func(x, e) => match x.eval()? {
                Expression::Variable(x) => Ok(Expression::Func(
                    Box::new(Expression::Variable(x)), // Enforce Func Variable variant again
                    e.clone(),                         // Do not evaluate body before substitution
                )),
                _ => Err("Variable operation 'func' given non-variable".to_owned()),
            },
            Expression::Apply(func, arg) => match func.eval()? {
                Expression::Func(param, body) => body.substitute(&param, &arg.eval()?)?.eval(),
                _ => Err("'func' operation 'apply' given non-'func'".to_owned()),
            },
            Expression::If(cond, e1, e2) => match cond.eval()? {
                Expression::True => e1.eval(),
                Expression::False => e2.eval(),
                _ => Err("Boolean condition in 'if' given non-boolean".to_owned()),
            },
        }
    }

    fn substitute(&self, param: &Expression, arg: &Expression) -> Result<Expression, String> {
        match self {
            Expression::Variable(_) if self == param => Ok(arg.clone()),
            Expression::Variable(x) => Ok(Expression::Variable(*x)),
            Expression::Int(i) => Ok(Expression::Int(*i)),
            Expression::True => Ok(Expression::True),
            Expression::False => Ok(Expression::False),
            Expression::LessThan(e1, e2) => Ok(Expression::LessThan(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::Equals(e1, e2) => Ok(Expression::Equals(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::Add(e1, e2) => Ok(Expression::Add(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::Subtract(e1, e2) => Ok(Expression::Subtract(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::Multiply(e1, e2) => Ok(Expression::Multiply(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::Divide(e1, e2) => Ok(Expression::Divide(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::And(e1, e2) => Ok(Expression::And(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::Or(e1, e2) => Ok(Expression::Or(
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
            Expression::Not(e) => Ok(Expression::Not(Box::new(e.substitute(param, arg)?))),
            // Allow for shadowing by ending substitution if current Func Variable matches param
            Expression::Func(x, _) if **x == *param => Ok(self.clone()),
            Expression::Func(x, e) => Ok(Expression::Func(
                x.clone(),                           // Do not substitute Func Variables
                Box::new(e.substitute(param, arg)?), // Substitute within nested Func body if not shadowed
            )),
            Expression::Apply(func, e) => Ok(Expression::Apply(
                Box::new(func.substitute(param, arg)?),
                Box::new(e.substitute(param, arg)?),
            )),
            Expression::If(cond, e1, e2) => Ok(Expression::If(
                Box::new(cond.substitute(param, arg)?),
                Box::new(e1.substitute(param, arg)?),
                Box::new(e2.substitute(param, arg)?),
            )),
        }
    }
}
