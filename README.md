# amap-maps-flutter
Flutter 使用高德地图SDK示例





iOS 使用注意事项

~~~
Trying to embed a platform view but the PrerollContext does not support embedding
~~~

Info.plist io.flutter.embedded_views_preview  YES


在原插件基础上添加聚合点方法：

#### addCluster(MarkerOptions options, Color color, String name,String count)
```
MarkerOptions options = MarkerOptions.defaultOptions;
mapController.addCluster(options, Colors.red, "内蒙古", "200");
```
