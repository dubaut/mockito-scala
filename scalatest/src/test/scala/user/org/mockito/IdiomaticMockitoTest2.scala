package user.org.mockito

import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{ Matchers, WordSpec }

class IdiomaticMockitoTest2 extends WordSpec with Matchers with IdiomaticMockito with ArgumentMatchersSugar with TableDrivenPropertyChecks {

  "mock" should {

    "correctly stub an invocation with concrete values" in {
      val myService = mock[Org]

      myService.defaultParams("hello", defaultParam2 = 3, defaultParam1 = true) returns "hello3true"

      myService.defaultParams("hello", defaultParam2 = 3, defaultParam1 = true) shouldBe "hello3true"
      myService.defaultParams("hello", defaultParam1 = true, defaultParam2 = 3) shouldBe "hello3true"
    }

    "correctly stub an invocation with matchers and concrete values" in {
      val myService = mock[Org]

      myService.defaultParams("hello", defaultParam2 = *, defaultParam1 = true) returns "hello3true"

      myService.defaultParams("hello", defaultParam2 = 3, defaultParam1 = true) shouldBe "hello3true"
      myService.defaultParams("hello", defaultParam1 = true, defaultParam2 = 3) shouldBe "hello3true"
    }

    "correctly stub an invocation dependent values" in {
      val myService = mock[Org]

      myService.curriedDefaultParams("hello", defaultParam1 = true)() returns "hello3true"

      myService.curriedDefaultParams("hello", defaultParam1 = true)() shouldBe "hello3true"
    }

    "correctly stub an invocation dependent values with default params applied" in {
      val myService = mock[Org]

      myService.curriedDefaultParams("hello")() returns "hello3true"

      myService.curriedDefaultParams("hello")() shouldBe "hello3true"
    }

  }

}
