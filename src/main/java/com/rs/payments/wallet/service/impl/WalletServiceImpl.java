package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.WalletService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(UserRepository userRepository,
                             WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Wallet createWalletForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getWallet() != null) {
            throw new BadRequestException("User already has a wallet");
        }

        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(user);
        user.setWallet(wallet);

        user = userRepository.save(user); // Cascade saves wallet
        return user.getWallet();
    }

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {

        // 1️⃣ Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        // 2️⃣ Find wallet
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // 3️⃣ Update balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // 4️⃣ Create transaction
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);

        transactionRepository.save(transaction);

        // 5️⃣ Return updated wallet
        return wallet;
    }
    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {

        // 1️⃣ Validate amount > 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        // 2️⃣ Find wallet
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // 3️⃣ Check sufficient balance
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }

        // 4️⃣ Deduct balance (never goes negative)
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // 5️⃣ Create transaction record
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.WITHDRAWAL);

        transactionRepository.save(transaction);

        return wallet;
    }

    @Override
    @Transactional
    public void transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        if (fromWalletId.equals(toWalletId)) {
            throw new BadRequestException("Cannot transfer to the same wallet");
        }

        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }

        // Debit sender
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        walletRepository.save(fromWallet);

        // Credit receiver
        toWallet.setBalance(toWallet.getBalance().add(amount));
        walletRepository.save(toWallet);

        // Transaction: TRANSFER_OUT
        Transaction debitTx = new Transaction();
        debitTx.setWallet(fromWallet);
        debitTx.setAmount(amount);
        debitTx.setType(TransactionType.TRANSFER_OUT);
        transactionRepository.save(debitTx);

        // Transaction: TRANSFER_IN
        Transaction creditTx = new Transaction();
        creditTx.setWallet(toWallet);
        creditTx.setAmount(amount);
        creditTx.setType(TransactionType.TRANSFER_IN);
        transactionRepository.save(creditTx);
    }
}