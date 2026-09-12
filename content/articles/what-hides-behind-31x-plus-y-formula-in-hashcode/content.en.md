The method `Objects.hash(Object... values)` uses `Arrays.hashCode()`, and the `hashCode()` implementation in `Arrays` looks like this:
```
public static int hashCode(Object a[]) {
    if (a == null) return 0;
    int result = 1;
    for (Object element : a)
        result = 31 * result + (element == null ? 0 : element.hashCode());
    return result;
}
```

We can also take `AbstractList.hashCode()` as an example:
```
public int hashCode() {
    int hashCode = 1;
    for(E e : this) {
        hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
    }
    return hashCode;
}
```

As you can see, the formula **`31 * x + y`** shows up in these implementations, and in this article I will try to answer two questions:
* **What happens if we remove the multiplier 31?**
* **Why exactly 31?**

... and at the end I will explain what weakness the current formula has and how HashMap makes up for it.

---


## Question 1: what happens if we remove the multiplier 31?

If we drop the multiplier, three problems show up:

### 1. Symmetry => collision on swap

```
hash(1, 2) = 3
hash(2, 1) = 3
```

Point (1,2) and point (2,1) are different objects, but they get the same hash. An unavoidable collision.

***Swapping the order of the terms does not change the sum.***

With strings it is even clearer - if `String.hashCode()` were just the sum of the characters, then `"abc"`, `"acb"`, `"bac"`, `"bca"`, `"cab"`, `"cba"` would all have **the same hash**. Addition does not see the order.

### 2. Range collapse

Let x, y be in [0, 100]. Total pairs: 101 squared = **10 201**.
 
But `x + y` can only take values from 0 to 200 - that is **201 different values**.
 
So 10 201 objects get spread across 201 buckets: on average **~50 objects per bucket** instead of one. HashMap degrades.
 
For comparison: `31 * x + y` on the same inputs gives values up to `31 * 100 + 100 = 3200` - an order of magnitude wider, and as long as `y < 31`, the mapping is fully one-to-one: every pair gets a **unique** hash.

![hash-code-range](images/hash-code-range.png)

### 3. A triangle shape instead of an even spread

Not only are there just 201 values - they are also spread unevenly, in a **triangle**:
 
* sum 0 => 1 way: (0, 0)
* sum 100 => 101 ways: (0,100), (1,99), ... (100,0)
* sum 200 => 1 way: (100, 100)

  ![hash-sum-distribution](images/hash-sum-distribution.png)

The picture shows that the middle buckets are packed while the edge ones are almost empty. But a hash function must be **even**.

### Takeaway

If you replace `31 * x + y` with `x + y`, the contract of `hashCode()` is formally **not broken** - the contract only requires ***equal objects => equal hashes***, the reverse is not required.
 
Technically even `return 0;` is a valid `hashCode()`. It just turns HashMap into a linked list with O(n) lookup. The contract is about **correctness**, while the quality of the spread is about **performance**.

---


## Question 2: why exactly 31?

The answer from Joshua Bloch in the book ***Effective Java (3rd edition)***, Item 11:
 
> The value 31 was chosen because it is an odd prime.
> If it were even and the multiplication overflowed, information would be lost, because multiplication by 2 is equivalent to shifting.
> The advantage of using a prime is less clear, but it is traditional.
> A nice property of 31 is that the multiplication can be replaced by a shift and a subtraction for better performance on some architectures: `31 * i == (i << 5) - i`.
> Modern virtual machines do this sort of optimization automatically.

Notice: **Bloch himself admits that the prime argument is weak** - he writes "less clear, but traditional". Let us go through all the arguments and see which ones actually hold up.

### Argument 1: odd number - the only strict one


This is math. Multiplying by an even number loses bits during the shift for good. Because `every multiply by 2 = shift of 1 bit to the left.`

So if we take the 32-bit form of the number 13 and multiply it by the smallest even number, 2:

```
13     = 0000 0000 0000 0000 0000 0000 0000 1101
13 * 2 = 0000 0000 0000 0000 0000 0000 0001 1010
                                               ^
                                   lowest bit = 0 guaranteed
```

The bits shifted left, and a zero **was added on the right**. And here is the key point: it is not that "the high bits flew out" (overflow happens with any multiplier, including 31 - that is normal), it is that **the low bits get filled with zeros, and there is nothing left to write there**. And the larger the power of 2, the faster the high bits are lost in exchange for zero low bits.

```
n * 2   = n << 1   => 1 low bit guaranteed 0
n * 4   = n << 2   => 2 low bits guaranteed 0
n * 32  = n << 5   => 5 low bits guaranteed 0
```

After each accumulation step, a dead zone of zeros grows in the low bits. And HashMap picks the bucket **using exactly the low bits** (`h & (n - 1)`) - so it looks right at the part of the number that we zeroed out with our own hands.
 
Important: the problem is not only with powers of two. Multiplying by 6 (= 2 * 3) also loses one bit, because 6 is divisible by 2.
 
This is the only argument that is **proven** rather than just stated.
   
### Argument 2: the `(i << 5) - i` optimization

The compiler turns `31 * x` into a shift and a subtraction:
 
```
31 = 32 - 1
31 * x = (x << 5) - x
```

Using the number 13 as an example:

```
13      = 0000 0000 0000 0000 0000 0000 0000 1101   (13)
13 << 5 = 0000 0000 0000 0000 0000 0001 1010 0000   (416)
 
  0000 0000 0000 0000 0000 0001 1010 0000   (416)
- 0000 0000 0000 0000 0000 0000 0000 1101   (13)
= 0000 0000 0000 0000 0000 0001 1001 0011   (403)
```

Notice the result: the lowest bit is **1**. No zero "tail" showed up - exactly because 31 is odd. Compare with the multiply by 2 above, where the lowest bit is guaranteed 0.

**But today this is not an argument in favor of 31.** Because modern virtual machines do this optimization (turning a multiply into shifts for any constant) automatically.

### Argument 3: the size

The multiplier decides **how fast the contribution of an old field is pushed past the 32-bit edge**. The larger the multiplier, the bigger the shift per step.

* **Too small a multiplier** (say, 3):
  A shift of only ~1.6 bits per step. The fields build up slowly, the high bits stay zero for a long time. Hashes cluster in a narrow range, and a large part of the 32-bit space is not used.

* **Too large a multiplier** (say, 1000003):
  A shift of ~20 bits per step. After just 2 steps the contribution of an early field is pushed past the 32-bit edge. The hash starts to depend **only on the last 1-2 fields**. Strings with the same ending collapse into one hash.

* **31 is about 2^5 - the sweet spot:**
  A shift of ~5 bits per step (log2 of 31 is about 4.95). For a 32-bit word this means: **a field's contribution lives about 6-7 steps** before it is pushed out.

**Why is 6-7 good?** It is a balance between two needs:

* fast enough to fill all 32 bits with entropy within a few characters (so no dead zero zones are left, like with a multiplier of 3);
* slow enough that a field's contribution does not die after a single step (like with 1000003).
  
For typical strings and objects with 3-5 fields, a window of 6-7 steps lands right in the useful range.

---

## What this formula cannot do
 
An important note that people usually stay quiet about.
 
A good hash function should have an **avalanche effect**: flipping one input bit flips each output bit with a probability of about 0.5.
 
`31 * h + c` does **not** have this property. The multiply spreads bits only **to the left** - the high bits of the result never affect the low ones. The mixing is one-way.
 
Real hash functions fix this with a final mixing step. For example, murmur3 runs the number through a mix of multiplies and right shifts:
 
```java
h ^= h >>> 16;
h *= 0x85ebca6b;
h ^= h >>> 13;
h *= 0xc2b2ae35;
h ^= h >>> 16;
```
 
The XOR with a **right** shift brings information from the high bits back into the low ones - and that is the missing half of the mixing.  (A full walk through murmur3 is a topic for a separate article, here it only matters that it does what `31 * x` cannot.)

---

## How HashMap makes up for the weakness of the formula

And here is the most interesting part. **HashMap knows about the weakness of `31 * x` and covers for it on its own.** To see how, we first need to understand how HashMap picks a bucket.

### The bucket is picked from the low bits
 
Inside HashMap there is an array of buckets (boxes). Say there are 16 of them. Which of the 16 do we put a key into? The obvious choice is `hash % 16`, but the remainder operation is slow. So for sizes that are a power of two, a fast trick is used:
 
```java
index = hash & (16 - 1)     // = hash & 15 = hash & 0b1111
```
 
`& 15` means "take only the last 4 bits", everything else is thrown away.
 
Here is the catch: the choice of bucket depends **only on the low bits** of the hash. And we just found out - for `31 * x` the low bits are the poorest and most predictable. So by default HashMap would look right where there is the least value, while the high bits (where all the entropy is) would not take part in picking the bucket at all.

### The line that fixes everything
 
Look at the private method `HashMap.hash()`:
 
```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

All the magic is in `h ^ (h >>> 16)`, which is a trimmed murmur finalizer. Let us break it down:
 
* `h >>> 16` - shift the number 16 bits **to the right**. The high 16 bits move down into the place of the low ones.
* `h ^ (...)` - the XOR mixes these rich high bits into the poor low bits.
Let us go through a real number. Say the hash is `0xA3B8_002C` (notice: the low half is almost empty - exactly the typical trouble with `31 * x`):

```
h        = 1010 0011 1011 1000   0000 0000 0010 1100
h >>> 16 = 0000 0000 0000 0000   1010 0011 1011 1000
                                 the high bits moved down
XOR:
h        = 1010 0011 1011 1000   0000 0000 0010 1100
h>>>16   = 0000 0000 0000 0000   1010 0011 1011 1000
----------------------------------------------------
result   = 1010 0011 1011 1000   1010 0011 1001 0100
                        the low bits NOW hold an "echo" of the high ones
```
 
Let us check that this really changes the chosen bucket (we take the last 4 bits):
 
```
before mixing:   0xA3B8002C & 15 = 12   => bucket 12
after mixing:    result & 15 = 4        => bucket 4
``` 

The line really changed the bucket - because it mixed in the info from the high half, which `& 15` would otherwise just throw away.

### Why exactly `>>> 16`
 
The number is 32-bit, and a shift of exactly half (16) folds the top half onto the bottom half - the most mixing for one cheap action. One shift plus one XOR, and that is it. HashMap is called millions of times, so every instruction matters: they took the cheapest trick that clearly improves bad hashes and barely hurts good ones.


### The tie to murmur3
 
Look closely: `h ^ (h >>> 16)` is literally the **first line** of the murmur3 finalizer from the previous section, just without the multiplies. The JDK took the cheapest piece from murmur - just enough to fix the main problem (the entropy leaning into the high bits), without paying for a full hash function.
 
The main idea:
 
> HashMap **does not trust** your `hashCode()`. It assumes that you (or `String`, or `Integer`) put all the entropy into the high bits, and it covers for that - it mixes them into the low bits, because when picking a bucket it will look at exactly the low bits.

----
## Wrap-up

**31 is not the best value, it is a historically fixed reasonable compromise.** The numbers 33, 37, or 131 would work just as well.
 
But there is a nuance: `String.hashCode()` is **fixed in the Java spec**. The JavaDoc writes down the exact formula `s[0]*31^(n-1) + ... + s[n-1]`, which means `"cat".hashCode()` must return 98262 in any version of the JDK, on any platform, always. It is part of the public contract - over 20+ years people have built systems on this (sharding, persistent caches), and you cannot break the promise.
 
But in **your own** classes you are not tied to anything. The contract only requires "equal objects => equal hashes". You can take 33, murmur3, xxHash - and you will be right. The fact that everyone writes 31 is a cultural norm, not a technical rule.

And what a hash that has the avalanche effect from the start looks like - murmur3 - I will cover in a separate article.