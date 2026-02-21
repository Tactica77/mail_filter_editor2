package jp.d77.java.mfe2.BasicIO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolParams {
    public static class StringArray {
        Map<String, List<String>> data_work = new HashMap<>();

        public void clear(){
            this.data_work.clear();
        }

        public void add( String key, String value ){
            if ( ! this.data_work.containsKey(key) ) this.data_work.put( key, new ArrayList<>() );
            this.data_work.get(key).add(value);
        }

        public String[] gets( String key ){
            if ( ! this.data_work.containsKey(key) ) return new String[0];
            return this.data_work.get(key).toArray( new String[0] );
        }

        public List<String> toArray( String key ){
            if ( ! this.data_work.containsKey(key) ) return new ArrayList<>();
            return this.data_work.get(key);
        }

        public String join( String delimiter,  String key ){
            return String.join( delimiter, this.gets(key) );
        }
    }

    public static class ArrayCounter {
        Map<String, Map<String, Integer>> data_cnt = new HashMap<>();

        public void add( String key, String item ){
            this.add( key, item, 1 );
        }

        public void add( String key, String item, Integer i ){
            if ( i == null ) return;
            if ( ! this.data_cnt.containsKey(key) ) this.data_cnt.put( key, new HashMap<>() );
            if ( ! this.data_cnt.get(key).containsKey( item ) ) this.data_cnt.get(key).put( item , 0 );
            this.data_cnt.get(key).put( item , this.data_cnt.get(key).get(item) + i );
        }

        public int get( String key, String item ){
            if ( ! this.data_cnt.containsKey(key) ) return 0;
            if ( ! this.data_cnt.get(key).containsKey( item ) ) return 0;
            return this.data_cnt.get(key).get(item);
        }
    }
}
