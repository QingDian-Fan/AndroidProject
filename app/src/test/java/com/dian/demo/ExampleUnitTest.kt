package com.dian.demo

import android.util.Log
import org.junit.Test

import org.junit.Assert.*
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test() {
        var i = 0;
        var name = 100;
        val map = ConcurrentHashMap<Int, Int>()
        val removeKeys = arrayListOf<Int>()
        val addKeys = arrayListOf<Int>()
        map[name] = 0
        for (i in 0..9) {
            map.forEach {
                val value = (it.value + 1);
                val key = it.key;
                map[key] = value
                if ((value % 2 == 0) && value != 0) {
                    name -= 1
                    addKeys.add(name)
                }
                if (value >= 5) {
                    removeKeys.add(key)
                }
            }
            addKeys.forEach {
                map[it] = 0
            }
            addKeys.clear()
            removeKeys.forEach {
                map.remove(it)
            }
            removeKeys.clear()
        }
        System.out.println("---->" + map.size)
    }


    var name = 100

    @Test
    fun testMain() {
        val map = ConcurrentHashMap<Int, Int>()
        map[name] = 0
        doIt(map, 1)
    }

    private fun doIt(map: ConcurrentHashMap<Int, Int>, year: Int) {
        val removeKeys = arrayListOf<Int>()
        val addKeys = arrayListOf<Int>()
        map.forEach {
            val value = it.value + 1;
            map[it.key] = value
            if (value % 2 == 0 && value != 0) {
                name -= 1
                addKeys.add(name)
            }
            if (value == 5) {
                removeKeys.add(it.key)
            }
        }
        addKeys.forEach {
            map[it] = 0
        }
        removeKeys.forEach {
            map.remove(it)
        }
        addKeys.clear()
        removeKeys.clear()
        if (year == 10) {
            println("---->" + map.size)
        } else {
            doIt(map, year + 1)
        }
    }


    @Test
    fun doRemoveElement() {
        val nums = intArrayOf(1, 1, 3, 4, 5, 2, 3)
        removeElement(nums, 1)
    }

    private fun removeElement(nums: IntArray, value: Int): Int {
        if (nums.isEmpty()) return 0
        if (nums.contains(value)) {
            var mIndex = 0
            for ((index, num) in nums.withIndex()) {
                if (num != value) {
                    nums[mIndex] = nums[index]
                    mIndex++
                }
            }
            return mIndex
        }
        return nums.size
    }


    @Test
    fun main() {
        val s = "Hello World"
        lengthOfLastWord2(s)
        //nums = [1,3,5,6], target = 5

        searchInsert(intArrayOf(1, 3, 5, 6), 5)
    }


    fun searchInsert(nums: IntArray, target: Int): Int {
        if (target <= nums[0]) return 0
        for (i in nums.indices) {
            if (nums[i] >= target) {
                return i
            }
        }
        return nums.size
    }

    fun plusOne(digits: IntArray): IntArray {

        if (digits.isEmpty()) return digits
        val index = digits.size - 1
        if (digits[index] != 9) {
            digits[index] = digits[index] + 1
            return digits
        }
        val sbf = StringBuilder()
        digits.forEach {
            sbf.append(it)
        }
        val bigDecimal = BigDecimal(sbf.toString()).plus(BigDecimal(1))
        val charArray = bigDecimal.toString().toCharArray()
        val array = IntArray(charArray.size)
        for (i in charArray.indices) {
            array[i] = Integer.valueOf(charArray[i].toString())
        }
        return array
    }

    fun lengthOfLastWord(s: String): Int {
        val trim = s.trim()
        val index = trim.lastIndexOf(" ")
        return trim.length - 1 - index
    }

    private fun lengthOfLastWord2(s: String): Int {
        val charArray = s.toCharArray()
        var length = 0
        for (i in charArray.size - 1 downTo 0) {
            if (charArray[i] == ' ' && length != 0) {
                return length
            } else if (charArray[i] != ' ') {
                length++
            }
        }
        return length
    }


}