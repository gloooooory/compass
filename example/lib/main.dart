import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:compass/compass.dart';
//import 'package:permission_handler/permission_handler.dart';
import 'dart:math' as math;

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
           /*    ElevatedButton(
                onPressed: () async => Permission.location.request(),
                child: const Text(
                  'Request Permission for true heading in Android',
                ),
              ), */
              StreamBuilder<double>(
                stream: Compass.heading,
                initialData: 0,
                builder: (_, AsyncSnapshot<double> snapshot) {
                  if (!snapshot.hasData) return const Text('Loading...');
                  log('heading:${snapshot.data.toString()}');
                  return Transform.rotate(
                    angle: snapshot.data! * math.pi / 180,
                    child: Icon(
                      Icons.arrow_upward_rounded,
                      size: MediaQuery.of(context).size.width - 80,
                    ),
                  );
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
