-- Counts failed AI parsing attempts so InvoiceScheduler stops retrying an invoice after a limit.
ALTER TABLE invoice ADD COLUMN parse_attempts INTEGER NOT NULL DEFAULT 0;
