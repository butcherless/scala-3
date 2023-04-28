package com.cmartin.learn

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.cmartin.learn.validation.ValidationPill.AccountModel.*
import com.cmartin.learn.validation.CatsAccountValidator

class CatsValidationPillSpec
    extends AnyFlatSpec
    with Matchers:

  import ValidationPillSpec.*

  behavior of "AccountValidation"

  "IBAN control" should "validate an account with valid data" in {
    val bankAccountView = BankAccountView(validIbanControl, validBank, validBranch, validControl, validNumber)

    val result = CatsAccountValidator.validate(bankAccountView)

  }
