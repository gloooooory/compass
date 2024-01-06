import 'package:flutter/material.dart';
import 'package:compass/compass.dart';

void main() => runApp(const App());

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              StreamBuilder<double>(
                stream: Compass.heading,
                initialData: 0,
                builder: (_, AsyncSnapshot<double> snapshot) {
                  if (!snapshot.hasData) return const Text('Loading...');
                  return Text('heading:${snapshot.data.toString()}');
                },
              ),
              StreamBuilder<bool>(
                stream: Compass.shouldCalibrate,
                initialData: false,
                builder: (_, AsyncSnapshot<bool> snapshot) {
                  if (!snapshot.hasData) return const Text('Loading...');
                  return Text('shouldCalibrate:${snapshot.data.toString()}');
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
