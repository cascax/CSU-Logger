package net.mccode.szzn.rsa;

import java.util.Arrays;

public class BigInt
{
	private int maxDigits = 130;
	private boolean ifNeg;

	public int[] digits;
	
	BigInt()
	{
		digits = new int[maxDigits];
	}
	/**
	 * 拷贝构造函数
	 * @param num
	 */
	BigInt(BigInt num)
	{
		this.digits = Arrays.copyOf(num.digits, this.maxDigits);
	}
	/**
	 * 反转负数标志
	 */
	public void reverseIfNeg()
	{
		this.ifNeg = ! ifNeg;
	}
	
	public int getMaxDigits()
	{
		return maxDigits;
	}
	public boolean isIfNeg()
	{
		return ifNeg;
	}
	public void setIfNeg(boolean ifNeg)
	{
		this.ifNeg = ifNeg;
	}
}
