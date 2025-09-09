-- Create catalog_items table with item_id as primary key
CREATE TABLE IF NOT EXISTS catalog_items (
    item_id VARCHAR(255) PRIMARY KEY,
    item_name TEXT,
    provider_id VARCHAR(255) NOT NULL,
    item_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) DEFAULT 'system',
    updated_by VARCHAR(255) DEFAULT 'system'
);

-- Create indexes for performance (only if they don't exist)
CREATE INDEX IF NOT EXISTS idx_catalog_items_provider_id ON catalog_items(provider_id);
CREATE INDEX IF NOT EXISTS idx_catalog_items_item_name ON catalog_items USING GIN(to_tsvector('english', item_name));
CREATE INDEX IF NOT EXISTS idx_catalog_items_created_at ON catalog_items(created_at);
CREATE INDEX IF NOT EXISTS idx_catalog_items_data ON catalog_items USING GIN(item_data);

-- Add constraints (only if they don't exist)
DO $$ 
BEGIN
    BEGIN
        ALTER TABLE catalog_items ADD CONSTRAINT catalog_items_provider_id_not_empty CHECK (provider_id != '');
    EXCEPTION
        WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER TABLE catalog_items ADD CONSTRAINT catalog_items_item_id_not_empty CHECK (item_id != '');
    EXCEPTION
        WHEN duplicate_object THEN NULL;
    END;
END $$;

-- Add updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for updated_at
CREATE TRIGGER update_catalog_items_updated_at 
    BEFORE UPDATE ON catalog_items 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE catalog_items IS 'Stores Beckn catalog items with raw JSON data';
COMMENT ON COLUMN catalog_items.item_id IS 'Beckn item identifier (primary key)';
COMMENT ON COLUMN catalog_items.provider_id IS 'Beckn provider identifier';
COMMENT ON COLUMN catalog_items.item_data IS 'Raw Beckn item JSON data';
