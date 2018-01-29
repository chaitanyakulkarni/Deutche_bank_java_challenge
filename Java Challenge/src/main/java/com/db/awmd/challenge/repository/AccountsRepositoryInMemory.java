package com.db.awmd.challenge.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class AccountsRepositoryInMemory implements AccountsRepository {

	private final Map<String, Account> accounts = new ConcurrentHashMap<>();

	@Autowired
	private NotificationService notificationService;

	@Override
	public void createAccount(Account account) throws DuplicateAccountIdException {
		Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
		if (previousAccount != null) {
			throw new DuplicateAccountIdException("Account id " + account.getAccountId() + " already exists!");
		}
	}

	/**
	 * Complete the transaction with amount transfer.
	 * This Method is synchronized so that multiple threads do not corrupt the data.
	 * @param fromAccount
	 * @param toAccount
	 * @param amountToTransfer
	 */
	public synchronized void transferBetweenAccounts(Account fromAccount, Account toAccount, BigDecimal amountToTransfer) {
		log.info("Initiating Transfer process for Accounts {}, {}", fromAccount.getAccountId(), toAccount.getAccountId());
		boolean isDeductionSuccess = deductFromAccount(fromAccount, amountToTransfer);
		if (isDeductionSuccess) {
			boolean isTransferSuccess = transferToAccount(fromAccount, toAccount, amountToTransfer);
			if (isTransferSuccess) {
				log.info("Completed Transfer process for Accounts {}, {}", fromAccount.getAccountId(), toAccount.getAccountId());
			} else {
				//If for some reason the deduction failed then revert back.
				//This can happen due to environmental issues such as Network issues, Constraint errors from DB etc.
				rollBackTransaction(fromAccount, amountToTransfer);
			}
		} else {
			log.info("Transfer could not be processed for Accounts {}, {}", fromAccount.getAccountId(), toAccount.getAccountId());
		}
	}


	/**
	 * Roll back the deducted amount.
	 * @param account
	 * @param amountToTransfer
	 */
	private void rollBackTransaction(Account account, BigDecimal amountToTransfer) {
		log.info("Reverting Deduction of amount to transfer:{} from Account {}:", amountToTransfer,
				account.getAccountId());
		BigDecimal sourceBalance = account.getBalance();

		// Subtract from source the amount to be deducted
		sourceBalance = sourceBalance.add(amountToTransfer);
		account.setBalance(sourceBalance.setScale(6, RoundingMode.HALF_EVEN));
		// update the transaction details
		accounts.put(account.getAccountId(), account);
		notificationService.notifyAboutTransfer(account,
				String.format("Reverted deducted amount %s from Account %s. The new balance is %s",
						amountToTransfer.toString(), account.getAccountId(), account.getBalance().toString()));
	}

	private boolean deductFromAccount(Account fromAccount, BigDecimal amountToTransfer) {
		boolean isSuccess = false;
		log.info("Deducting amount to transfer:{} from Account {}:", amountToTransfer, fromAccount.getAccountId());
		BigDecimal sourceBalance = fromAccount.getBalance();

		// Subtract from source the amount to be deducted
		sourceBalance = sourceBalance.subtract(amountToTransfer);
		fromAccount.setBalance(sourceBalance.setScale(6, RoundingMode.HALF_EVEN));
		// update the transaction details
		accounts.put(fromAccount.getAccountId(), fromAccount);
		//We are here means the Account got successfully updated.
		isSuccess = true;
		log.info("Successfully deducted amount to transfer:{} from Account {}:", amountToTransfer, fromAccount.getAccountId());
		notificationService.notifyAboutTransfer(fromAccount,
				String.format("Successfully deducted amount %s from Account %s. The new balance is %s",
						amountToTransfer.toString(), fromAccount.getAccountId(), fromAccount.getBalance().toString()));
		return isSuccess;
	}

	private boolean transferToAccount(Account fromAccount, Account toAccount, BigDecimal amountToTransfer) {
		boolean isSuccess = false;
		log.info("Adding amount to transfer:{} to Account {}:", amountToTransfer, toAccount.getAccountId());
		BigDecimal toAccountBalance = toAccount.getBalance();
		// Add to target account the amount to be transferred
		toAccountBalance = toAccountBalance.add(amountToTransfer);
		toAccount.setBalance(toAccountBalance.setScale(6, RoundingMode.HALF_EVEN));

		// update the transaction details
		accounts.put(toAccount.getAccountId(), toAccount);
		//We are here means the Account got successfully updated.
		isSuccess = true;
		log.info("Successfully Added amount to transfer:{} to Account {}:", amountToTransfer, toAccount.getAccountId());
		notificationService.notifyAboutTransfer(toAccount,
				String.format("Successfully Received amount %s from Account %s. The new balance is %s",
						amountToTransfer.toString(), fromAccount.getAccountId(), toAccount.getBalance().toString()));
		
		return isSuccess;
	}

	@Override
	public Account getAccount(String accountId) {
		return accounts.get(accountId);
	}

	@Override
	public void clearAccounts() {
		accounts.clear();
	}

}
