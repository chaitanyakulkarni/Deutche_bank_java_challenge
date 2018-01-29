package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.NoAccountIdProvidedException;
import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	@Before
	public void setup() {
		this.accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	public void addAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	public void addAccount_failsOnDuplicateId() throws Exception {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}

	}

	@Test
	public void testTransferNoFromAccount() {
		Account accountFrom = new Account("ACC_1", new BigDecimal(1000));
		Account accountTo = new Account("ACC_2", new BigDecimal(500));
		
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		
		try {
			this.accountsService.transferBetweenAccounts("", "ACC_2", new BigDecimal(250.00));
			fail("Should Fail when creating account with invalid From Account Id");
		} catch (NoAccountIdProvidedException noAccountIdProvidedException) {
			assertThat(noAccountIdProvidedException.getMessage())
					.isEqualTo("From Account Id must be provided in order for transfer to happen.");
		}
	}

	@Test
	public void testTransferNoToAccount() {
		
		Account accountFrom = new Account("ACC_1", new BigDecimal(1000));
		Account accountTo = new Account("ACC_2", new BigDecimal(500));
		
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		
		try {
			this.accountsService.transferBetweenAccounts("ACC_1", "", new BigDecimal(250.00));
			fail("Should have failed when no transfer to account was provided");
		} catch (NoAccountIdProvidedException noAccountIdProvidedException) {
			assertThat(noAccountIdProvidedException.getMessage())
					.isEqualTo("To Account Id must be provided in order for transfer to happen.");
		}
	}

	@Test
	public void testTransferNoAmountToTransfer() {
		
		Account accountFrom = new Account("ACC_1", new BigDecimal(1000));
		Account accountTo = new Account("ACC_2", new BigDecimal(500));
		
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		
		try {
			this.accountsService.transferBetweenAccounts("ACC_1", "ACC_2", null);
			fail("Should have failed when inavlid transfer amount.");
		} catch (InvalidAmountException invalidAmountException) {
			assertThat(invalidAmountException.getMessage())
					.isEqualTo("Amount To Transfer must be positive and greater than 0.");
		}
	}

	@Test
	public void testTransferBetweenAccounts_Success() {
		Account accountFrom = new Account("ACC_1", new BigDecimal(1000));
		Account accountTo = new Account("ACC_2", new BigDecimal(500));
		
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		
		this.accountsService.transferBetweenAccounts("ACC_1", "ACC_2", new BigDecimal(250.00));
		assertThat(this.accountsService.getAccount("ACC_2").getBalance()).isEqualTo(new BigDecimal(750.00).setScale(6, RoundingMode.HALF_EVEN));
	}
}
