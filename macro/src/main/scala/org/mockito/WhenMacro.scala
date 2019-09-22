package org.mockito

import org.mockito.Utils._
import org.mockito.stubbing.{ScalaFirstStubbing, ScalaOngoingStubbing}

import scala.reflect.ClassTag
import scala.reflect.macros.blackbox

object WhenMacro {

  private def transformInvocation(c: blackbox.Context)(invocation: c.Tree): c.Tree = {
    import c.universe._

    val pf: PartialFunction[c.Tree, c.Tree] = {
      case q"$obj.$method[..$targs](...$args)" =>
        val newArgs = args.map(a => transformArgs(c)(a))
        q"$obj.$method[..$targs](...$newArgs)"
      case q"$obj.$method[..$targs]" => invocation
    }

    if (pf.isDefinedAt(invocation))
      pf(invocation)
    else if (pf.isDefinedAt(invocation.children.last)) {
      val values = invocation.children
        .dropRight(1)
        .collect {
          case q"$_ val $name:$_ = $value" => name.toString -> value.asInstanceOf[c.Tree]
        }
        .toMap

      val nonMatchers = invocation.children.dropRight(1).collect {
        case t @ q"$_ val $_:$_ = $value" if !isMatcher(c)(value) => t
      }

      invocation.children.last match {
        case q"$obj.$method[..$targs](...$args)" =>
          val newArgs = args.map { a =>
            transformArgs(c)(a).map {
              case p if show(p).startsWith("x$") => transformArg(c)(values(p.toString))
              case other                         => other
            }
          }
          q"..$nonMatchers; $obj.$method[..$targs](...$newArgs)"
      }
    } else throw new Exception(s"Couldn't recognize invocation ${show(invocation)}")
  }

  private val ShouldReturnOptions            = Set("shouldReturn", "mustReturn", "returns")
  private val FunctionalShouldReturnOptions  = ShouldReturnOptions.map(_ + "F")
  private val FunctionalShouldReturnOptions2 = ShouldReturnOptions.map(_ + "FG")
  def shouldReturn[T: c.WeakTypeTag](c: blackbox.Context): c.Tree = {
    import c.universe._

    val r = c.macroApplication match {
      case q"$_.StubbingOps[$t]($invocation).$m" if ShouldReturnOptions.contains(m.toString) =>
        q"new _root_.org.mockito.IdiomaticMockitoBase.ReturnActions(_root_.org.mockito.Mockito.when[$t](${transformInvocation(c)(invocation)}))"

      case q"$_.$cls[..$_]($invocation).$m" if cls.toString.startsWith("StubbingOps") && FunctionalShouldReturnOptions.contains(m.toString) =>
        q"new _root_.org.mockito.${packageName(c)(cls)}.${className(c)(cls, "IdiomaticMockito")}.ReturnActions(_root_.org.mockito.Mockito.when(${transformInvocation(c)(invocation)}))"

      case q"$_.$cls[..$_]($invocation).$m" if cls.toString.startsWith("StubbingOps2") && FunctionalShouldReturnOptions2.contains(m.toString) =>
        q"new _root_.org.mockito.${packageName(c)(cls)}.${className(c)(cls, "IdiomaticMockito")}.ReturnActions2(_root_.org.mockito.Mockito.when(${transformInvocation(c)(invocation)}))"

      case o => throw new Exception(s"Couldn't recognize ${show(o)}")
    }
    if (c.settings.contains("mockito-print-when")) println(show(r))
    r
  }

  def isLenient[T: c.WeakTypeTag](c: blackbox.Context)(): c.Expr[Unit] = {
    import c.universe._

    val r = c.Expr[Unit] {
      c.macroApplication match {
        case q"$_.StubbingOps[$t]($invocation).isLenient()" =>
          q"new _root_.org.mockito.stubbing.ScalaFirstStubbing(_root_.org.mockito.Mockito.when[$t](${transformInvocation(c)(invocation)})).isLenient()"

        case o => throw new Exception(s"Couldn't recognize ${show(o)}")
      }
    }
    if (c.settings.contains("mockito-print-lenient")) println(show(r.tree))
    r
  }

  object RealMethod {
    def willBe(called: Called.type): Called.type = called
  }

  val ShouldCallOptions = Set("shouldCall", "mustCall", "calls")
  def shouldCallRealMethod[T: c.WeakTypeTag](c: blackbox.Context)(crm: c.Expr[RealMethod.type]): c.Expr[ScalaOngoingStubbing[T]] = {
    import c.universe._

    val r = c.Expr[ScalaOngoingStubbing[T]] {
      c.macroApplication match {
        case q"$_.StubbingOps[$t]($invocation).$m($_.realMethod)" if ShouldCallOptions.contains(m.toString) =>
          q"new _root_.org.mockito.stubbing.ScalaOngoingStubbing(_root_.org.mockito.Mockito.when[$t](${transformInvocation(c)(invocation)}).thenCallRealMethod())"

        case o => throw new Exception(s"Couldn't recognize ${show(o)}")
      }
    }
    if (c.settings.contains("mockito-print-when")) println(show(r.tree))
    r
  }

  private val ShouldThrowOptions           = Set("shouldThrow", "mustThrow", "throws")
  private val FunctionalShouldFailOptions  = Set("shouldFailWith", "mustFailWith", "failsWith", "raises")
  private val FunctionalShouldFailOptions2 = Set("shouldFailWithG", "mustFailWithG", "failsWithG", "raisesG")
  def shouldThrow[T: c.WeakTypeTag](c: blackbox.Context): c.Tree = {
    import c.universe._

    val r = c.macroApplication match {
      case q"$_.StubbingOps[$t]($invocation).$m" if ShouldThrowOptions.contains(m.toString) =>
        q"new _root_.org.mockito.IdiomaticMockitoBase.ThrowActions(_root_.org.mockito.Mockito.when[$t](${transformInvocation(c)(invocation)}))"

      case q"$_.$cls[..$_]($invocation).$m" if cls.toString.startsWith("StubbingOps") && FunctionalShouldFailOptions.contains(m.toString) =>
        q"new _root_.org.mockito.${packageName(c)(cls)}.${className(c)(cls, "IdiomaticMockito")}.ThrowActions(_root_.org.mockito.Mockito.when(${transformInvocation(c)(invocation)}))"

      case q"$_.$cls[..$_]($invocation).$m" if cls.toString.startsWith("StubbingOps2") && FunctionalShouldFailOptions2.contains(m.toString) =>
        q"new _root_.org.mockito.${packageName(c)(cls)}.${className(c)(cls, "IdiomaticMockito")}.ThrowActions2(_root_.org.mockito.Mockito.when(${transformInvocation(c)(invocation)}))"

      case o => throw new Exception(s"Couldn't recognize ${show(o)}")
    }
    if (c.settings.contains("mockito-print-when")) println(show(r))
    r
  }

  class AnswerActions[T](os: ScalaFirstStubbing[T]) {
    def apply(f: => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0](f: P0 => T)(implicit classTag: ClassTag[P0] = defaultClassTag[P0]): ScalaOngoingStubbing[T] =
      os thenAnswer f

    def apply[P0, P1](f: (P0, P1) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2](f: (P0, P1, P2) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2, P3](f: (P0, P1, P2, P3) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2, P3, P4](f: (P0, P1, P2, P3, P4) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2, P3, P4, P5](f: (P0, P1, P2, P3, P4, P5) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2, P3, P4, P5, P6](f: (P0, P1, P2, P3, P4, P5, P6) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2, P3, P4, P5, P6, P7](f: (P0, P1, P2, P3, P4, P5, P6, P7) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2, P3, P4, P5, P6, P7, P8](f: (P0, P1, P2, P3, P4, P5, P6, P7, P8) => T): ScalaOngoingStubbing[T] = os thenAnswer f

    def apply[P0, P1, P2, P3, P4, P5, P6, P7, P8, P9](f: (P0, P1, P2, P3, P4, P5, P6, P7, P8, P9) => T): ScalaOngoingStubbing[T] =
      os thenAnswer f

    def apply[P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10](f: (P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => T): ScalaOngoingStubbing[T] =
      os thenAnswer f
  }

  private val ShouldAnswerOptions            = Set("shouldAnswer", "mustAnswer", "answers")
  private val FunctionalShouldAnswerOptions  = ShouldAnswerOptions.map(_ + "F")
  private val FunctionalShouldAnswerOptions2 = ShouldAnswerOptions.map(_ + "FG")
  def shouldAnswer[T: c.WeakTypeTag](c: blackbox.Context): c.Tree = {
    import c.universe._

    val r = c.macroApplication match {
      case q"$_.StubbingOps[$t]($invocation).$m" if ShouldAnswerOptions.contains(m.toString) =>
        q"new _root_.org.mockito.WhenMacro.AnswerActions(_root_.org.mockito.Mockito.when[$t](${transformInvocation(c)(invocation)}))"

      case q"$_.$cls[..$_]($invocation).$m" if cls.toString.startsWith("StubbingOps") && FunctionalShouldAnswerOptions.contains(m.toString) =>
        q"new _root_.org.mockito.${packageName(c)(cls)}.${className(c)(cls, "IdiomaticMockito")}.AnswerActions(_root_.org.mockito.Mockito.when(${transformInvocation(c)(invocation)}))"

      case q"$_.$cls[..$_]($invocation).$m" if cls.toString.startsWith("StubbingOps2") && FunctionalShouldAnswerOptions2.contains(m.toString) =>
        q"new _root_.org.mockito.${packageName(c)(cls)}.${className(c)(cls, "IdiomaticMockito")}.AnswerActions2(_root_.org.mockito.Mockito.when(${transformInvocation(c)(invocation)}))"

      case o => throw new Exception(s"Couldn't recognize ${show(o)}")
    }
    if (c.settings.contains("mockito-print-when")) println(show(r))
    r
  }
}
