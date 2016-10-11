package test.map

import test.Store

/**
 * Created by marcoscarceles on 07/10/2016.
 */
class MapTest {

    static searchable = {
        only = ['strings', 'stores', 'moreStrings', 'moreStores']
    }

    // These fields are troublesome with Hibernate, but not with other GORM implementations, ie. MongoDB.
    // That's why we test them too
    static transients = ['moreStrings', 'moreStores']

    Map<String, String> strings
    Map<String, Store> stores
    Map<String, List<String>> moreStrings
    Map<String, List<Store>> moreStores

    static hasMany = [strings: String, stores:Store, moreStrings: String, moreStores:Store]
}
