package com.dian.demo

import android.util.Log
import org.junit.Test

import org.junit.Assert.*
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
}