# hustle

A github and bitbucket activity graph mashup.

## Development Mode

```
lein dev
node target/server_dev/index.js
```

Figwheel will automatically push cljs changes to the browser and node server.

Wait a bit, then browse to [http://localhost:3779](http://localhost:3779).

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build

@TODO
