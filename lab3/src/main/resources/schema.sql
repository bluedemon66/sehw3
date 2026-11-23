CREATE TABLE IF NOT EXISTS shops (
  shop_id UUID PRIMARY KEY,
  merchant_id UUID NOT NULL
);
CREATE TABLE IF NOT EXISTS shop_riders (
  shop_id UUID REFERENCES shops(shop_id),
  rider_id UUID,
  PRIMARY KEY (shop_id, rider_id)
);
CREATE TABLE IF NOT EXISTS dishes (
  dish_id UUID PRIMARY KEY,
  shop_id UUID NOT NULL REFERENCES shops(shop_id),
  price DECIMAL(14, 2) NOT NULL CHECK (price >= 0)
);
CREATE TABLE IF NOT EXISTS orders (
  order_id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  shop_id UUID NOT NULL REFERENCES shops(shop_id),
  rider_id UUID NOT NULL,
  status VARCHAR(40) NOT NULL,
  paid_amount DECIMAL(14, 2) NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  version BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS order_items (
  order_id UUID REFERENCES orders(order_id),
  dish_id UUID REFERENCES dishes(dish_id),
  quantity INT NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (order_id, dish_id)
);
CREATE TABLE IF NOT EXISTS exception_cases (
  case_id UUID PRIMARY KEY,
  order_id UUID NOT NULL UNIQUE REFERENCES orders(order_id),
  status VARCHAR(12) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  resolution VARCHAR(2000),
  resolved_at TIMESTAMP WITH TIME ZONE
);
CREATE TABLE IF NOT EXISTS idempotency (
  actor_id UUID NOT NULL,
  request_id UUID NOT NULL,
  digest BINARY(32) NOT NULL,
  response BLOB,
  PRIMARY KEY (actor_id, request_id)
);
CREATE TABLE IF NOT EXISTS resources (
  resource_id UUID PRIMARY KEY,
  owner_id UUID NOT NULL,
  media_type VARCHAR(40) NOT NULL,
  content_length BIGINT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS comments (
  comment_id UUID PRIMARY KEY,
  target_kind VARCHAR(24) NOT NULL,
  target_id UUID NOT NULL,
  shop_id UUID,
  order_id UUID,
  author_id UUID NOT NULL,
  author_label VARCHAR(100) NOT NULL,
  body VARCHAR(4000) NOT NULL,
  body_length INT NOT NULL,
  image_count INT NOT NULL,
  purchase_mark VARCHAR(24) NOT NULL,
  status VARCHAR(24) NOT NULL,
  moderation_reason VARCHAR(2000),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS comments_listing ON comments(target_kind, target_id, status, body_length, created_at);
CREATE TABLE IF NOT EXISTS comment_images (
  comment_id UUID REFERENCES comments(comment_id),
  resource_id UUID REFERENCES resources(resource_id),
  position INT NOT NULL,
  PRIMARY KEY (comment_id, resource_id),
  UNIQUE (comment_id, position)
);
CREATE TABLE IF NOT EXISTS comment_replies (
  reply_id UUID PRIMARY KEY,
  comment_id UUID NOT NULL REFERENCES comments(comment_id),
  merchant_label VARCHAR(100) NOT NULL,
  body VARCHAR(4000) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
