package com.cmartin.learn.validation

import com.cmartin.learn.validation.ValidationPill.AccountModel.BankAccountView
import com.cmartin.learn.validation.ValidationPill.AccountModel.ValidationError

import cats.data.ValidatedNec
import com.cmartin.learn.validation.ValidationPill.AccountModel.*
import cats.implicits._

object CatsAccountValidator {

  def validate(view: BankAccountView): ValidatedNec[ValidationError, BankAccountView] = {
    val x1 = validateIbanControl(view.iban)
    val x2 = validateBankCode(view.bank)
    val x3 = validateBranchCode(view.branch)
    val x4 = validateNumberControl(view.control, view.number)
    val x5 = validateNumber(view.number)
    (x1, x2, x3, x4, x5).mapN(BankAccountView.apply)

  }
  // .mapN(BankAccountView)

  private def validateIbanControl(control: IbanControl): ValidatedNec[ValidationError, IbanControl] = ???
  private def validateNumberControl(
      control: NumberControl,
      number: AccountNumber
  ): ValidatedNec[ValidationError, NumberControl] = ???

  private def validateBankCode(code: BankCode): ValidatedNec[ValidationError, BankCode]           = ???
  private def validateBranchCode(branch: BranchCode): ValidatedNec[ValidationError, BranchCode]   = ???
  private def validateNumber(number: AccountNumber): ValidatedNec[ValidationError, AccountNumber] = ???

}
