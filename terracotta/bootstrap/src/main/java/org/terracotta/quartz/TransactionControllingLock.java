/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.quartz;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.terracotta.toolkit.ToolkitFeatureTypeInternal;
import org.terracotta.toolkit.atomic.ToolkitTransaction;
import org.terracotta.toolkit.atomic.ToolkitTransactionController;
import org.terracotta.toolkit.atomic.ToolkitTransactionType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.internal.ToolkitInternal;

/**
 *
 * @author cdennis
 */
class TransactionControllingLock implements ToolkitLock {

  private final ThreadLocal<HoldState> threadState = new ThreadLocal<HoldState>() {
    @Override
    protected HoldState initialValue() {
      return new HoldState();
    }
  };
  private final ToolkitTransactionController txnController;
  private final ToolkitTransactionType txnType;
  private final ToolkitLock delegate;
  
  public TransactionControllingLock(ToolkitInternal toolkit, ToolkitLock lock, ToolkitTransactionType txnType) {
    this.txnController = toolkit.getFeature(ToolkitFeatureTypeInternal.TRANSACTION);
    this.txnType = txnType;
    this.delegate = lock;
  }

  @Override
  public Condition newCondition() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Condition getCondition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitLockType getLockType() {
    return delegate.getLockType();
  }

  @Override
  public boolean isHeldByCurrentThread() {
    return delegate.isHeldByCurrentThread();
  }

  @Override
  public void lock() {
    delegate.lock();
    threadState.get().lock();
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    delegate.lockInterruptibly();
    threadState.get().lock();
  }

  @Override
  public boolean tryLock() {
    if (delegate.tryLock()) {
      threadState.get().lock();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    if (delegate.tryLock(time, unit)) {
      threadState.get().lock();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void unlock() {
    threadState.get().unlock();
    delegate.unlock();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  class HoldState {
    
    private ToolkitTransaction txnHandle;
    private int holdCount = 0;
    
    void lock() {
      if (holdCount++ == 0) {
        if (txnHandle == null) {
          System.out.println("Starting Transaction On " + Thread.currentThread());
          txnHandle = txnController.beginTransaction(txnType);
        } else {
          throw new AssertionError();
        }
      }
    }

    void unlock() {
      if (--holdCount <= 0) {
        System.out.println("Commiting Transaction On " + Thread.currentThread());
        try {
          txnHandle.commit();
        } finally {
          threadState.remove();
        }
      }
    }
  }
}
