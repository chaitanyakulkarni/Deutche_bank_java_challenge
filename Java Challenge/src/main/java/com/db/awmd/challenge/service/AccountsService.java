package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InSufficientFundsException;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.NoAccountIdProvidedException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.NumberUtils;

@Service
@Slf4j
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository) {
		this.accountsRepository = accountsRepository;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public void transferBetweenAccounts(String fromAccountId, String toAccountId, BigDecimal amountToTransfer)
			throws NoAccountIdProvidedException, InvalidAmountException, InSufficientFundsException {
		
		Account fromAccount = null;
		Account toAccount = null;
		
		if (fromAccountId != null && !fromAccountId.isEmpty()) {
			fromAccount = getAccount(fromAccountId);
		}
		
		if (toAccountId != null && !toAccountId.isEmpty()) {
			toAccount = getAccount(toAccountId); 
		}
		 
		validateAccountDetails(fromAccount, toAccount, amountToTransfer);
		this.accountsRepository.transferBetweenAccounts(fromAccount, toAccount, amountToTransfer);
	}

	/**
	 * Validate the passed account details.
	 * 
	 * @param fromAccount
	 * @param toAccount
	 * @param amountToTransfer
	 * @throws NoAccountIdProvidedException
	 * @throws InvalidAmountException
	 * @throws InSufficientFundsException
	 */
	private void validateAccountDetails(Account fromAccount, Account toAccount, BigDecimal amountToTransfer)
			throws NoAccountIdProvidedException, InvalidAmountException, InSufficientFundsException {
		log.info("Validating Account details.");
		if (fromAccount == null || fromAccount.getAccountId() == null) {
			throw new NoAccountIdProvidedException("From Account Id must be provided in order for transfer to happen.");
		}

		if (toAccount == null || toAccount.getAccountId() == null) {
			throw new NoAccountIdProvidedException("To Account Id must be provided in order for transfer to happen.");
		}
		
		if (amountToTransfer == null || amountToTransfer.compareTo(BigDecimal.ZERO) == 0
				|| amountToTransfer.signum() < 0) {
			throw new InvalidAmountException("Amount To Transfer must be positive and greater than 0.");
		}

		if (fromAccount.getBalance().compareTo(BigDecimal.ZERO) <= 0
				|| fromAccount.getBalance().compareTo(amountToTransfer) < 0) {
			throw new InSufficientFundsException(String.format("Insuffient Funds %s at Source account %s for transfer.",
					fromAccount.getBalance().toString(), fromAccount.getAccountId()));
		}
		log.info("Finished validations");
	}

}
