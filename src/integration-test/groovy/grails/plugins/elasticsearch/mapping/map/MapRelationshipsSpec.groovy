package grails.plugins.elasticsearch.mapping.map

import grails.plugins.elasticsearch.ElasticSearchAdminService
import grails.plugins.elasticsearch.ElasticSearchHelper
import grails.plugins.elasticsearch.ElasticSearchService
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.metadata.MappingMetaData
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import test.Store
import test.map.MapTest


/**
 * Created by marcoscarceles on 07/10/2016.
 */
@Integration
@Rollback
class MapRelationshipsSpec extends Specification {

    @Autowired
    ElasticSearchHelper elasticSearchHelper
    @Autowired
    ElasticSearchService elasticSearchService
    @Autowired
    ElasticSearchAdminService elasticSearchAdminService

    def "Maps of strings are properly mapped"() {
        expect: 'The index to have been properly generated'
        elasticSearchHelper.withElasticSearch { Client client ->
            client.admin().indices().prepareExists('test.map_v0').execute().actionGet().exists
        }

        and: 'The type to have a "strings" field'
        elasticSearchHelper.withElasticSearch { Client client ->
            GetMappingsResponse response = client.admin().indices().prepareGetMappings('test.map_v0').addTypes('mapTest').execute().actionGet()
            MappingMetaData mappingMetaData = response.mappings().get('test.map_v0').get('mapTest')
            Map mapping = mappingMetaData.sourceAsMap()
            mapping.properties.containsKey('strings') &&
            mapping.properties.strings.type == 'object'
        }

        when: "Indexing a MapTest"
        MapTest test = new MapTest(strings: ['1' : 'one', '2' : 'two'])
        test.save()
        and:
        elasticSearchService.index(test)
        elasticSearchAdminService.refresh(MapTest)

        then: "It exists on the index"
        elasticSearchHelper.withElasticSearch { Client client ->
            client.prepareSearch('test.map_v0').setTypes('mapTest').setQuery(QueryBuilders.matchAllQuery()).setSize(0).execute().actionGet().hits.totalHits == 1
        }

        when: "Querying for it with the raw client"
        Map source = elasticSearchHelper.withElasticSearch { Client client ->
            client.prepareGet('test.map_v0', 'mapTest', test.id as String).execute().actionGet().sourceAsMap
        }

        then: "It keeps the expected structure"
        source.keySet().contains('strings')
        source['strings'].keySet() == ['1', '2'] as Set
        source['strings']['1'] == 'one'
        source['strings']['2'] == 'two'

        when: "Using the Plugin Services"
        Map search = MapTest.search('one two')

        then:
        search.total == 1
        search.searchResults[0].strings instanceof Map
        search.searchResults[0].strings == test.strings
    }

    def "Maps of objects are indexed properly"() {

        when:
        Store tesco = new Store(name: 'Tesco', owner: 'Jack Cohen', description: 'The Tesco brand first appeared in 1924. The name came about after Jack Cohen bought a shipment of tea from Thomas Edward Stockwell.')
        Store greggs = new Store(name: 'Greggs', owner: 'John Gregg', description: 'Greggs was founded by John Gregg as a Tyneside bakery in 1939.')
        MapTest test = new MapTest(strings: ['1' : 'one', '2' : 'two'])
        test.stores = [supermarket: tesco, bakery: greggs]
        test.save(failOnError:true)

        and:
        elasticSearchService.index(test)
        elasticSearchAdminService.refresh(MapTest)

        then:
        Map search = MapTest.search('tesco greggs')
        search.total == 1
        MapTest returned = search.searchResults[0]
        returned.stores.size() == 2
        returned.stores.keySet() == ['supermarket', 'bakery'] as Set
        returned.stores['supermarket'] instanceof Store
        returned.stores['supermarket'].name == tesco.name
        returned.stores['supermarket'].description == tesco.description
        returned.stores['supermarket'].owner == tesco.owner
        returned.stores['bakery'] instanceof Store
        returned.stores['bakery'].name == greggs.name
        returned.stores['bakery'].description == greggs.description
        returned.stores['bakery'].owner == greggs.owner


    }

}
