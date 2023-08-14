package com.cmartin.learn

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DummyIntegrationSpec extends AnyFlatSpec with Matchers:

  behavior of "DummyIntegration"

  it should "pass" in {
    val result = true

    result shouldBe true
  }
