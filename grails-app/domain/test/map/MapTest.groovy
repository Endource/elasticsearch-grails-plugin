package test.map

import test.Store

/**
 * Created by marcoscarceles on 07/10/2016.
 */
class MapTest {

    static searchable = true

    Map<String, String> strings
    Map<String, Store> stores

    static hasMany = [strings: String, stores:Store]
}
