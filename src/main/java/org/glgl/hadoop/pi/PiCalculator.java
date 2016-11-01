package org.glgl.hadoop.pi;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.StringTokenizer;

public class PiCalculator {
    public static class DistanceMapper extends Mapper<Object, Text, Text, LongWritable> {

        private static final LongWritable one = new LongWritable(1);
        private Text inner = new Text("inner");
        private Text outer = new Text("outer");

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer tokenizer = new StringTokenizer(value.toString(), ",");

            double total = 0.0;

            while (tokenizer.hasMoreTokens()) {
                double d = Double.parseDouble(tokenizer.nextToken());
                total += d * d;
            }

            if (Math.sqrt(total) >= 1.0) {
                context.write(outer, one);
            } else {
                context.write(inner, one);
            }
        }
    }

    public static class LongSumReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
        private LongWritable result = new LongWritable();

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;
            for (LongWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String... args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(DistanceMapper.class);
        job.setMapperClass(DistanceMapper.class);
        job.setCombinerClass(LongSumReducer.class);
        job.setReducerClass(LongSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
