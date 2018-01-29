package com.db.awmd.challenge.web;

import java.math.BigDecimal;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InSufficientFundsException;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.NoAccountIdProvidedException;
import com.db.awmd.challenge.service.AccountsService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

	private final AccountsService accountsService;

	@Autowired
	public AccountsController(AccountsService accountsService) {
		this.accountsService = accountsService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
		log.info("Creating account {}", account);

		try {
			this.accountsService.createAccount(account);
		} catch (DuplicateAccountIdException daie) {
			return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping(path = "/{accountId}")
	public Account getAccount(@PathVariable String accountId) {
		log.info("Retrieving account for id {}", accountId);
		return this.accountsService.getAccount(accountId);
	}

	@PostMapping(path="/transferBetweenAccounts", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> transferBetweenAccounts(@RequestBody @Valid TransferRequest transferRequest) {
		log.info("Initiating transfer from {} to account {} for amount {}", transferRequest.getFromAccountId(), transferRequest.getToAccountId(),
				transferRequest.getAmountToTransfer());
		try {
			this.accountsService.transferBetweenAccounts(transferRequest.getFromAccountId(), transferRequest.getToAccountId(), transferRequest.getAmountToTransfer());
		} catch (NoAccountIdProvidedException noAccountIdProvidedException) {
			return new ResponseEntity<Object>(noAccountIdProvidedException.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (InvalidAmountException invalidAmountException) {
			return new ResponseEntity<Object>(invalidAmountException.getMessage(), HttpStatus.BAD_REQUEST);
		}  catch (InSufficientFundsException inSufficientFundsException) {
			return new ResponseEntity<Object>(inSufficientFundsException.getMessage(), HttpStatus.FORBIDDEN);
		}
		log.info("Completed transfer from {} to account {} for amount {}", transferRequest.getFromAccountId(), transferRequest.getToAccountId(),
				transferRequest.getAmountToTransfer());
		return new ResponseEntity<Object>(HttpStatus.OK);
	}

}
