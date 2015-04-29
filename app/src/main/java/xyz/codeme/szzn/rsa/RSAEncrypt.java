package xyz.codeme.szzn.rsa;

import java.util.Arrays;

/**
 * 数字中南密码加密类
 * 
 * key与modulus为常量，实例化以后可用encryptedString()来加密。
 * 
 * @author Msir
 * @date 2015/3
 * @example
 * 		RSAEncrypt.newInstance().encryptedString("123456");
 *
 */
public class RSAEncrypt
{
	private final int[] muArray = {64960,5794,58342,4906,21255,5449,37131,1520,25147,48127,
			15949,8219,32169,50821,10190,27537,14250,44660,20599,33058,37701,20652,
			45979,39567,20286,30758,17065,20293,58918,47896,54663,31950,53677,57426,
			6549,19452,14672,16668,37652,22641,44716,5187,8214,50719,46975,33250,60475,
			56069,45892,11879,33516,19745,41990,3787,2709,62927,59461,1988,64302,21380,
			25436,62171,55507,33957,1};
	private final int[] lowBitMasks  = {0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F,
							            0x003F, 0x007F, 0x00FF, 0x01FF, 0x03FF, 0x07FF,
							            0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF};
	private final String publicKey = "10001";
	private final String modulusHex = "a8a02b821d52d3d0ca90620c78474b78435423be99da83cc190ab5cb5b9b922a4c8ba6b251e78429757cf11cde119e1eacff46fa3bf3b43ef68ceb29897b7aa6b5b1359fef6f35f32b748dc109fd3d09f3443a2cc3b73e99579f3d0fe6a96ccf6a48bc40056a6cac327d309b93b1d61d6f6e8f4a42fc9540f34f1c4a2e053445";

	private int biRadixBits = 16;
	private int bitsPerDigit = 16;
	private int biRadix = 65536; // 2^16
	private int maxDigitVal = biRadix -1;
	
	private BigInt encryption; // e
	private BigInt modulus; // m
	private int chunkSize;
	
	private int k;
	private BigInt mu;
	private BigInt bkplus1;
	
	RSAEncrypt()
	{
		this.setEncryptKey(modulusHex);
	}
	
	public static RSAEncrypt newInstance()
	{
		return new RSAEncrypt();
	}
	
	/**
	 * 加密明文
	 * @param s
	 * @return
	 * 		密文
	 */
	public String encryptedString(String s)
	{
		if(modulus == null) return "";
		int sLen = s.length(),
			codeLen = this.chunkSize;
		if(sLen > this.chunkSize)
			codeLen = (sLen / this.chunkSize + 1) * this.chunkSize;
		int sCode[] = new int[codeLen];
		int i = 0;
		while(i < sLen) sCode[i] = s.codePointAt(i++);
		
		String result = "";
		int j, k;
		BigInt block;
		for(i = 0; i< codeLen; i += this.chunkSize)
		{
			block = new BigInt();
			j = 0;
			for(k = i; k < i + this.chunkSize; j++)
			{
				block.digits[j] = sCode[k++];
				block.digits[j] += sCode[k++] << 8;
			}
			BigInt crypt = powMod(block, this.encryption);
			String text = biToHex(crypt);
			result += text + " ";
		}
		return result.substring(0, result.length() - 1); // 去掉最后空格
	}
	
	/**
	 * 设置密钥
	 * @param modulus
	 * 		密钥
	 */
	private void setEncryptKey(String modulus)
	{
		this.encryption = this.biFromHex(publicKey);
		this.modulus = this.biFromHex(modulus);
		int modulusIndex = biHighIndex(this.modulus);
		this.chunkSize = 2 * modulusIndex;
		
		// BarrettMu
		k = modulusIndex + 1;
		mu = new BigInt();
		mu.digits = Arrays.copyOf(muArray, mu.getMaxDigits());
		bkplus1 = new BigInt();
		bkplus1.digits[k + 1] = 1; // bkplus1 = b^(k+1)
	}
	
	private BigInt powMod(BigInt x, BigInt y)
	{
		BigInt result = new BigInt();
		result.digits[0] = 1;
		BigInt a = new BigInt(x);
		BigInt k = new BigInt(y);
		while(true)
		{
			if((k.digits[0] & 1) != 0) result = multiplyMod(result, a);
			k = biShiftRight(k, 1);
			if (k.digits[0] == 0 && biHighIndex(k) == 0) break;
			a = multiplyMod(a, a);
		}
		return result;
	}
	
	private BigInt multiplyMod(BigInt x, BigInt y)
	{
		BigInt xy = biMultiply(x, y);
		
		BigInt q1 = biDivideByRadixPower(xy, this.k - 1);
		BigInt q2 = biMultiply(q1, this.mu);
		BigInt q3 = biDivideByRadixPower(q2, this.k + 1);
		BigInt r1 = biModuloByRadixPower(xy, this.k + 1);
		BigInt r2term = biMultiply(q3, this.modulus);
		BigInt r2 = biModuloByRadixPower(r2term, this.k + 1);
		BigInt r = biSubtract(r1, r2);
		if (r.isIfNeg())
			r = biAdd(r, this.bkplus1);
		boolean rgtem = biCompare(r, this.modulus) >= 0;
		while (rgtem)
		{
			r = biSubtract(r, this.modulus);
			rgtem = biCompare(r, this.modulus) >= 0;
		}
		return r;
	}
	/**
	 * 大数加法
	 * @param x
	 * @param y
	 * @return
	 */
	private BigInt biAdd(BigInt x, BigInt y)
	{
		BigInt result;
		if (x.isIfNeg() != y.isIfNeg())
		{
			y.reverseIfNeg();
			result = biSubtract(x, y);
			y.reverseIfNeg();
		}
		else
		{
			result = new BigInt();
			int n, c = 0;
			for (int i = 0; i < x.digits.length; i++)
			{
				n = x.digits[i] + y.digits[i] + c;
				result.digits[i] = n % biRadix;
				c = n >= biRadix ? 1 : 0;
			}
			result.setIfNeg(x.isIfNeg());
		}
		return result;
	}
	/**
	 * 大数减法
	 * @param x
	 * @param y
	 * @return
	 */
	private BigInt biSubtract(BigInt x, BigInt y)
	{
		BigInt result;
		if (x.isIfNeg() != y.isIfNeg())
		{
			y.reverseIfNeg();
			result = biAdd(x, y);
			y.reverseIfNeg();
		}
		else
		{
			result = new BigInt();
			int n, c = 0;
			for (int i = 0; i < x.digits.length; i++)
			{
				n = x.digits[i] - y.digits[i] + c;
				result.digits[i] = n % biRadix;
				if (result.digits[i] < 0) result.digits[i] += biRadix;
				c = n < 0 ? -1 : 0;
			}
			if (c == -1)
			{
				c = 0;
				for (int i = 0; i < x.digits.length; i++)
				{
					n = 0 - result.digits[i] + c;
					result.digits[i] = n % biRadix;
					if (result.digits[i] < 0) result.digits[i] += biRadix;
					c = n < 0 ? -1 : 0;
				}
				result.setIfNeg(! x.isIfNeg());
			}
			else
				result.setIfNeg(x.isIfNeg());
		}
		return result;
	}
	/**
	 * 大数乘法
	 * @param x
	 * @param y
	 * @return
	 */
	private BigInt biMultiply (BigInt x, BigInt y)
	{
		BigInt result = new BigInt();
		int	n = biHighIndex(x),
			t = biHighIndex(y);
		int uv, k, c;

		for (int i = 0; i <= t; ++i) {
			c = 0;
			k = i;
			for (int j = 0; j <= n; ++j, ++k) {
				uv = result.digits[k] + x.digits[j] * y.digits[i] + c;
				result.digits[k] = uv & maxDigitVal;
				c = uv >>> biRadixBits;
			}
			result.digits[i + n + 1] = c;
		}
		result.setIfNeg(x.isIfNeg() != y.isIfNeg());
		return result;
	}
	/**
	 * 大数比较
	 * @param x
	 * @param y
	 * @return
	 * 		x>y 1; x=y 0; x<y -1
	 */
	private int biCompare(BigInt x, BigInt y)
	{
		for(int i = x.getMaxDigits() -1; i >= 0; i--)
		{
			if(x.digits[i] < y.digits[i])
				return -1;
			else if(x.digits[i] > y.digits[i])
				return 1;
		}
		return 0;
	}
	/**
	 * 二进制右移
	 * @param x
	 * 		被右移大数
	 * @param n
	 * 		右移位数
	 * @return
	 */
	private BigInt biShiftRight(BigInt x, int n)
	{
		int digitCount = n / bitsPerDigit;
		BigInt result = new BigInt();
		arrayCopy(x, digitCount, result, 0,
				x.getMaxDigits() - digitCount); // 先每个元素整体移动
		int bits = n % bitsPerDigit;
		int leftBits = bitsPerDigit - bits;
		for (int i = 0, i1 = i + 1; i < result.digits.length - 1; ++i, ++i1)
		{
			result.digits[i] = (result.digits[i] >>> bits) |
			                   ((result.digits[i1] & lowBitMasks[bits]) << leftBits);
		}
		result.digits[result.digits.length - 1] >>>= bits;
		result.setIfNeg(x.isIfNeg());
		return result;
		
	}
	/**
	 * 数组拷贝
	 * @param src
	 * 		原数组
	 * @param srcStart
	 * 		原数组开始位置
	 * @param dest
	 * 		目标数组
	 * @param destStart
	 * 		目标数组开始位置
	 * @param n
	 * 		拷贝长度
	 */
	private void arrayCopy(BigInt src, int srcStart, BigInt dest, int destStart, int n)
	{
		int m = Math.min(srcStart + n, src.getMaxDigits());
		for(int i = srcStart, j = destStart; i < m; i++, j++)
			dest.digits[j] = src.digits[i];
	}
	
	private BigInt biDivideByRadixPower(BigInt x, int n)
	{
		BigInt result = new BigInt();
		arrayCopy(x, n, result, 0, result.getMaxDigits() - n);
		return result;
	};
	
	private BigInt biModuloByRadixPower(BigInt x, int n)
	{
		BigInt result = new BigInt();
		arrayCopy(x, 0, result, 0, n);
		return result;
	}
	/**
	 * 大数转为十六进制字符串
	 * @param x
	 * @return
	 */
	private String biToHex(BigInt x)
	{
		String result = "";
		String zero = "0000";
		for (int i = biHighIndex(x); i > -1; --i)
		{
			String hex = Integer.toHexString(x.digits[i]);
			result += zero.substring(hex.length()) + hex;
		}
		return result;
	}
	/**
	 * 获取大数最高位
	 * @param num
	 * @return
	 */
	private int biHighIndex(BigInt num)
	{
		int i = num.getMaxDigits() - 1;
		while(i > 0 && num.digits[i] == 0) i--;
		return i;
	}
	/**
	 * 十六进制转大数
	 * @param s
	 * @return
	 */
	private BigInt biFromHex(String s)
	{
		BigInt result = new BigInt();
		int len = s.length();
		for(int i=len, j = 0; i>0; i-=4, j++)
			result.digits[j] = Integer.parseInt(s.substring(Math.max(0, i-4), i), 16);
		return result;
	}
	
}
